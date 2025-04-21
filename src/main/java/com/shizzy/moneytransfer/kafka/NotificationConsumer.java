package com.shizzy.moneytransfer.kafka;

import com.shizzy.moneytransfer.dto.TransactionNotification;
import com.shizzy.moneytransfer.dto.TransferEmailDto;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.UserNotificationPreferences;
import com.shizzy.moneytransfer.repository.UserNotificationPreferencesRepository;
import com.shizzy.moneytransfer.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

    private EmailService emailService;

    private final UserNotificationPreferencesRepository userPreferencesRepository;

    @Autowired
    @Qualifier("brevoEmailService")
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "notifications", groupId = "notification_group")
    public void listen(ConsumerRecord<String, TransactionNotification> record) {

        TransactionNotification transactionNotification = record.value();

        logger.info("Consumed transaction notification: {}", transactionNotification);

        TransactionOperation operation = transactionNotification.getOperation();

        if (operation == TransactionOperation.TRANSFER) {
            handleTransferNotification(transactionNotification);
        } else {
            handleSingleTransactionNotification(transactionNotification);
        }

    }

    private void handleSingleTransactionNotification(TransactionNotification transactionNotification) {
        String userId = transactionNotification.getTransferInfo().getSenderId();
        String userEmail = transactionNotification.getTransferInfo().getSenderEmail();

        UserNotificationPreferences userPreferences = userPreferencesRepository.findByCreatedBy(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User notification preferences not found"));

        boolean shouldNotify = switch (transactionNotification.getOperation()) {
            case WITHDRAWAL -> userPreferences.isNotifyOnWithdraw();
            case DEPOSIT -> userPreferences.isNotifyOnDeposit();
            case TRANSFER, REVERSAL, REFUND -> false;
        };

        if (shouldNotify) {
            if (transactionNotification.getOperation() == TransactionOperation.WITHDRAWAL) {
                emailService.sendDebitTransactionEmail(
                        transactionNotification.getDebitTransaction(),
                        userEmail,
                        transactionNotification.getTransferInfo().getSenderName(),
                        "Debit Transaction Notification");
            } else {
                emailService.sendCreditTransactionEmail(
                        transactionNotification.getCreditTransaction(),
                        userEmail,
                        transactionNotification.getTransferInfo().getSenderName(),
                        "Credit Transaction Notification");
            }
        }
        logger.info("User preference on notification {}:", shouldNotify);
    }

    private void handleTransferNotification(TransactionNotification transactionNotification) {
        String senderId = transactionNotification.getTransferInfo().getSenderId();
        String receiverId = transactionNotification.getTransferInfo().getReceiverId();
        String senderEmail = transactionNotification.getTransferInfo().getSenderEmail();
        String receiverEmail = transactionNotification.getTransferInfo().getReceiverEmail();
        String senderName = transactionNotification.getTransferInfo().getSenderName();
        String receiverName = transactionNotification.getTransferInfo().getReceiverName();

        UserNotificationPreferences senderPreferences = userPreferencesRepository.findByCreatedBy(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender notification preferences not found"));

        UserNotificationPreferences receiverPreferences = userPreferencesRepository.findByCreatedBy(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver notification preferences not found"));


        if (senderPreferences.isNotifyOnSend()) {
            // Send debit notification to sender
            TransferEmailDto senderEmailDto = TransferEmailDto.builder()
                    .senderEmail(senderEmail)
                    .receiverEmail(receiverEmail)
                    .senderName(senderName)
                    .receiverName(receiverName)
                    .transaction(transactionNotification.getDebitTransaction())
                    .subject("Money Sent Notification")
                    .build();

            // Send debit notification to sender
            emailService.sendDebitTransferEmail(senderEmailDto);
            logger.info("Sent debit transfer email to sender: {}", senderEmail);
        }

        if (receiverPreferences.isNotifyOnReceive()) {
            // Create a separate DTO for the receiver
            TransferEmailDto receiverEmailDto = TransferEmailDto.builder()
                    .senderEmail(senderEmail)
                    .receiverEmail(receiverEmail)
                    .senderName(senderName)
                    .receiverName(receiverName)
                    .transaction(transactionNotification.getCreditTransaction())
                    .subject("Money Received Notification")
                    .build();

            // Send credit notification to receiver
            emailService.sendCreditTransferEmail(receiverEmailDto);
            logger.info("Sent credit transfer email to receiver: {}", receiverEmail);
        }
    }
}
