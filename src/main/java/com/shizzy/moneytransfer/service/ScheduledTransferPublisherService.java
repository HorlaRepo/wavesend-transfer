package com.shizzy.moneytransfer.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shizzy.moneytransfer.dto.ScheduledTransferMessage;
import com.shizzy.moneytransfer.enums.ScheduleStatus;
import com.shizzy.moneytransfer.kafka.NotificationProducer;
import com.shizzy.moneytransfer.model.ScheduledTransfer;
import com.shizzy.moneytransfer.repository.ScheduledTransferRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTransferPublisherService {

    private static final String TOPIC_SCHEDULED_TRANSFERS = "scheduled-transfers-execution";
    private static final int MAX_RETRY_COUNT = 3;
    private static final int BATCH_SIZE = 100;
    
    private final ScheduledTransferRepository scheduledTransferRepository;
    private final NotificationProducer notificationProducer;
    
    /**
     * Scan for scheduled transfers that are due for execution and publish them to Kafka
     * Runs every minute to catch due transfers
     */
    @Scheduled(fixedRate = 60000) // run every minute
    @Transactional(readOnly = true)
    public void publishDueTransfers() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lookAhead = now.plusMinutes(2); // look 2 minutes ahead
        
        log.debug("Scanning for scheduled transfers due between {} and {}", now, lookAhead);
        
        List<ScheduledTransfer> dueTransfers = scheduledTransferRepository
                .findDueUnprocessedTransfersInTimeWindow(ScheduleStatus.PENDING, now, lookAhead);
                
        if (!dueTransfers.isEmpty()) {
            log.info("Found {} transfers due for execution", dueTransfers.size());
            dueTransfers.forEach(this::publishTransferToKafka);
        }
    }
    
    /**
     * Scan for failed transfers that need to be retried
     * Runs every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    @Transactional(readOnly = true)
    public void retryFailedTransfers() {
        LocalDateTime retryBefore = LocalDateTime.now().minusMinutes(15); // retry transfers that failed at least 15 minutes ago
        
        List<ScheduledTransfer> failedTransfers = scheduledTransferRepository
                .findFailedTransfersForRetry(ScheduleStatus.FAILED, MAX_RETRY_COUNT, retryBefore);
                
        if (!failedTransfers.isEmpty()) {
            log.info("Retrying {} failed transfers", failedTransfers.size());
            failedTransfers.forEach(transfer -> publishTransferToKafka(transfer, "RETRY"));
        }
    }
    
    /**
     * Publish a specific transfer to Kafka
     * @param transfer The scheduled transfer to publish
     */
    public void publishTransferToKafka(ScheduledTransfer transfer) {
        publishTransferToKafka(transfer, "PUBLISH");
    }
    
    /**
     * Publish a specific transfer to Kafka with a custom message type
     * @param transfer The scheduled transfer to publish
     * @param messageType The type of message (PUBLISH, RETRY, RECOVERY)
     */
    public void publishTransferToKafka(ScheduledTransfer transfer, String messageType) {
        ScheduledTransferMessage message = ScheduledTransferMessage.builder()
                .transferId(transfer.getId())
                .senderEmail(transfer.getSenderEmail())
                .receiverEmail(transfer.getReceiverEmail())
                .amount(transfer.getAmount())
                .description(transfer.getDescription())
                .scheduledDateTime(transfer.getScheduledDateTime())
                .status(transfer.getStatus())
                .messageTimestamp(LocalDateTime.now())
                .parentTransferId(transfer.getParentTransferId())
                .isRecurring(transfer.getRecurrenceType() != null && 
                             !transfer.getRecurrenceType().toString().equals("NONE"))
                .messageType(messageType)
                .build();
                
        notificationProducer.sendNotification(TOPIC_SCHEDULED_TRANSFERS, message);
        log.info("Published transfer ID {} to Kafka (type: {})", transfer.getId(), messageType);
    }
    
    /**
     * Publish all missed transfers at application startup
     * This ensures no transfers are missed if the server was down
     */
    @Transactional(readOnly = true)
    public void publishMissedTransfers() {
        LocalDateTime now = LocalDateTime.now();
        
        // Get all transfers that were due before now but haven't been processed
        // Process in batches to avoid memory issues with large datasets
        int page = 0;
        boolean hasMore = true;
        int totalRecovered = 0;
        
        while (hasMore) {
            List<ScheduledTransfer> missedTransfers = scheduledTransferRepository
                    .findOverdueUnprocessedTransfers(
                        ScheduleStatus.PENDING, 
                        now,
                        PageRequest.of(page, BATCH_SIZE)
                    );
                    
            if (missedTransfers.isEmpty()) {
                hasMore = false;
            } else {
                totalRecovered += missedTransfers.size();
                log.info("Recovering batch {} of missed transfers ({} items)", page + 1, missedTransfers.size());
                
                missedTransfers.forEach(transfer -> 
                    publishTransferToKafka(transfer, "RECOVERY")
                );
                
                page++;
            }
        }
        
        if (totalRecovered > 0) {
            log.info("Recovered a total of {} missed transfers at startup", totalRecovered);
        } else {
            log.info("No missed transfers found at startup");
        }
    }
}