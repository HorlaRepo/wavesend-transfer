package com.shizzy.moneytransfer.kafka;

import java.time.LocalDateTime;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shizzy.moneytransfer.dto.CreateTransactionRequestBody;
import com.shizzy.moneytransfer.dto.ScheduledTransferMessage;
import com.shizzy.moneytransfer.dto.ScheduledTransferNotification;
import com.shizzy.moneytransfer.enums.RecurrenceType;
import com.shizzy.moneytransfer.enums.ScheduleStatus;
import com.shizzy.moneytransfer.exception.InsufficientBalanceException;
import com.shizzy.moneytransfer.model.ScheduledTransfer;
import com.shizzy.moneytransfer.repository.ScheduledTransferRepository;
import com.shizzy.moneytransfer.service.EmailService;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.MoneyTransferService;
import com.shizzy.moneytransfer.serviceimpl.ScheduledTransferServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTransferConsumer {

    private static final String TOPIC_NOTIFICATIONS = "scheduled-transfer-notifications";
    private static final int MAX_RETRY_COUNT = 3;

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final MoneyTransferService moneyTransferService;
    private final NotificationProducer notificationProducer;

    private final KeycloakService keycloakService;
    private EmailService emailService;
    private final ScheduledTransferServiceImpl scheduledTransferServiceImpl;

    @Autowired
    @Qualifier("brevoEmailService")
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "scheduled-transfers-execution", groupId = "scheduled_transfer_execution_group")
    @Transactional
    public void listen(ConsumerRecord<String, ScheduledTransferMessage> record) {
        ScheduledTransferMessage message = record.value();
        log.info("Received scheduled transfer execution message: id={}, type={}",
                message.getTransferId(), message.getMessageType());

        try {
            // Retrieve the latest version of the scheduled transfer
            ScheduledTransfer transfer = scheduledTransferRepository.findById(message.getTransferId())
                    .orElseThrow(
                            () -> new RuntimeException("Scheduled transfer not found: " + message.getTransferId()));

            // Skip already processed transfers or those with changed status
            if (transfer.getProcessed() == true || transfer.getStatus() != ScheduleStatus.PENDING) {
                log.info("Skipping transfer ID {} - already processed or status changed", transfer.getId());
                return;
            }

            // Execute the transfer
            executeTransfer(transfer);

        } catch (Exception e) {
            log.error("Error processing scheduled transfer: {}", e.getMessage(), e);

            // For system errors, mark the message for retry but don't increment the retry
            // count
            // This allows the system to recover from temporary failures
            if (!(e instanceof InsufficientBalanceException)) {
                handleSystemError(message);
            }
        }
    }

    /**
     * Execute a scheduled transfer
     */
    private void executeTransfer(ScheduledTransfer transfer) {
        try {
            // Mark as processing to prevent duplicate execution
            transfer.setStatus(ScheduleStatus.PROCESSING);
            scheduledTransferRepository.save(transfer);

            // Create and execute the actual money transfer
            CreateTransactionRequestBody requestBody = new CreateTransactionRequestBody(
                    transfer.getSenderEmail(),
                    transfer.getReceiverEmail(),
                    transfer.getAmount(),
                    transfer.getDescription());

            // Execute the transfer
            moneyTransferService.transfer(requestBody);

            // Update the transfer status to EXECUTED
            transfer.setStatus(ScheduleStatus.EXECUTED);
            transfer.setExecutedAt(LocalDateTime.now());
            transfer.setProcessed(true);
            transfer.setProcessedDateTime(LocalDateTime.now());
            scheduledTransferRepository.save(transfer);

            // Send notification
            sendNotification(transfer, "TRANSFER_EXECUTED");

            log.info("Successfully executed scheduled transfer ID: {}", transfer.getId());

            // If this is a recurring transfer, schedule the next occurrence
            if (transfer.getRecurrenceType() != RecurrenceType.NONE) {
                scheduledTransferServiceImpl.scheduleNextOccurrenceIfNeeded(transfer);
            }

            // Handle cache eviction to update the transfer status
            scheduledTransferServiceImpl.evictCachesAfterProcessing(
                    transfer.getId(),
                    transfer.getParentTransferId());

        } catch (InsufficientBalanceException e) {
            // Business failure - not enough funds
            handleInsufficientFunds(transfer, e.getMessage());

        } catch (Exception e) {
            // Technical failure
            handleExecutionFailure(transfer, e.getMessage());
            throw e; // rethrow to trigger retry
        }
    }

    /**
     * Handle the case where a transfer fails due to insufficient funds
     */
    private void handleInsufficientFunds(ScheduledTransfer transfer, String reason) {
        transfer.setStatus(ScheduleStatus.FAILED);
        transfer.setProcessed(true); // Mark as processed - this is a business failure, not technical
        transfer.setProcessedDateTime(LocalDateTime.now());
        transfer.setFailureReason("Insufficient funds: " + reason);
        scheduledTransferRepository.save(transfer);

        // Send failure notification
        sendNotification(transfer, "TRANSFER_FAILED");

        // Add cache eviction
        scheduledTransferServiceImpl.evictCachesAfterProcessing(
                transfer.getId(),
                transfer.getParentTransferId());

        log.info("Transfer ID {} failed due to insufficient funds", transfer.getId());
    }

    /**
     * Handle execution failures that may be retryable
     */
    private void handleExecutionFailure(ScheduledTransfer transfer, String reason) {
        transfer.setStatus(ScheduleStatus.FAILED);
        transfer.setRetryCount(transfer.getRetryCount() + 1);
        transfer.setLastRetryDateTime(LocalDateTime.now());
        transfer.setFailureReason(reason);

        if (transfer.getRetryCount() >= MAX_RETRY_COUNT) {
            // Max retries reached - mark as processed (failed)
            transfer.setProcessed(true);
            transfer.setProcessedDateTime(LocalDateTime.now());

            // Send failure notification after max retries
            sendNotification(transfer, "TRANSFER_FAILED");

            // Add cache eviction
            scheduledTransferServiceImpl.evictCachesAfterProcessing(
                    transfer.getId(),
                    transfer.getParentTransferId());

            log.warn("Transfer ID {} failed permanently after {} retries: {}",
                    transfer.getId(), transfer.getRetryCount(), reason);
        } else {
            log.warn("Transfer ID {} failed (retry {}/{}): {}",
                    transfer.getId(), transfer.getRetryCount(), MAX_RETRY_COUNT, reason);
        }

        scheduledTransferRepository.save(transfer);
    }

    /**
     * Handle system errors that prevent message processing
     */
    private void handleSystemError(ScheduledTransferMessage message) {
        // For system errors, we don't update the database yet
        // The message will be retried by Kafka's retry mechanism
        log.error("System error processing transfer ID {}, will be retried by Kafka",
                message.getTransferId());
    }

    /**
     * Send a notification for a scheduled transfer event
     */
    private void sendNotification(ScheduledTransfer transfer, String eventType) {
        // Create notification object
        ScheduledTransferNotification notification = ScheduledTransferNotification.builder()
                .scheduledTransferId(transfer.getId())
                .senderEmail(transfer.getSenderEmail())
                .receiverEmail(transfer.getReceiverEmail())
                .amount(transfer.getAmount().doubleValue())
                .scheduledDateTime(transfer.getScheduledDateTime())
                .description(transfer.getDescription())
                .recurrenceType(transfer.getRecurrenceType() != null ? transfer.getRecurrenceType().name() : null)
                .recurrenceEndDate(transfer.getRecurrenceEndDate())
                .totalOccurrences(transfer.getTotalOccurrences())
                .status(transfer.getStatus())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .build();

        // Send to Kafka topic for notification processing
        notificationProducer.sendNotification(TOPIC_NOTIFICATIONS, notification);
        log.info("Sent {} notification for scheduled transfer ID: {}", eventType, transfer.getId());
    }
}