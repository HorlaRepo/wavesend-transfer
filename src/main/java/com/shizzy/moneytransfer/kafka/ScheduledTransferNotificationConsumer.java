package com.shizzy.moneytransfer.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.shizzy.moneytransfer.dto.ScheduledTransferEmailDto;
import com.shizzy.moneytransfer.dto.ScheduledTransferNotification;
import com.shizzy.moneytransfer.enums.EmailTemplateName;
import com.shizzy.moneytransfer.service.EmailService;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.ScheduledTransferService;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTransferNotificationConsumer {

    private static final String TOPIC_NOTIFICATIONS = "scheduled-transfer-notifications";

    private EmailService emailService;
    private final KeycloakService keycloakService;
    private final ScheduledTransferService scheduledTransferService;

    @Autowired
    @Qualifier("brevoEmailService")
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = TOPIC_NOTIFICATIONS, groupId = "scheduled_transfer_notification_group")
    public void listen(ConsumerRecord<String, ScheduledTransferNotification> record) {
        ScheduledTransferNotification notification = record.value();
        log.info("Received notification: id={}, type={}",
                notification.getScheduledTransferId(), notification.getEventType());

        try {
            processNotification(notification);
        } catch (Exception e) {
            log.error("Error processing notification: {}", e.getMessage(), e);
        }
    }

    private void processNotification(ScheduledTransferNotification notification) {
        EmailTemplateName templateName = getTemplateForEventType(notification.getEventType());

        if (templateName != null) {

            Map<String, Object> templateModel = new HashMap<>();

            // Add all required template fields
            templateModel.put("sender_name", getSenderName(notification.getSenderEmail()));
            templateModel.put("receiver_name", getReceiverName(notification.getReceiverEmail()));
            templateModel.put("sender_email", notification.getSenderEmail());
            templateModel.put("receiver_email", notification.getReceiverEmail());
            templateModel.put("currency_symbol", "$"); // Default currency symbol
            templateModel.put("amount", notification.getAmount().toString());
            templateModel.put("transfer_id", notification.getScheduledTransferId().toString());
            templateModel.put("scheduled_date", formatDateTime(notification.getScheduledDateTime()));
            templateModel.put("description",
                    notification.getDescription() != null ? notification.getDescription() : "Money Transfer");
            templateModel.put("status", notification.getStatus().toString());

            // Check if this is a recurring transfer
            boolean isRecurring = notification.getRecurrenceType() != null &&
                    !notification.getRecurrenceType().equals("NONE");
            templateModel.put("is_recurring", isRecurring);

            if (isRecurring) {
                templateModel.put("recurrence_type", notification.getRecurrenceType());

                if (notification.getRecurrenceEndDate() != null) {
                    templateModel.put("recurrence_end", formatDateTime(notification.getRecurrenceEndDate()));
                } else if (notification.getTotalOccurrences() != null) {
                    templateModel.put("recurrence_end", "After " + notification.getTotalOccurrences() + " occurrences");
                }
            }

            // Add social media URLs
            templateModel.put("dashboard_url", "http://localhost:4200/account/scheduled-transfers");
            templateModel.put("facebook_url", "https://facebook.com/wavesend");
            templateModel.put("twitter_url", "https://twitter.com/wavesend");
            templateModel.put("instagram_url", "https://instagram.com/wavesend");
            templateModel.put("current_year", String.valueOf(LocalDateTime.now().getYear()));

            try {
                // Send the email with the complete template model
                emailService.sendEmail(
                        notification.getSenderEmail(),
                        templateModel,
                        templateName,
                        getSubjectForEventType(notification.getEventType()));

                log.info("Sent {} notification email to {}",
                        notification.getEventType(), notification.getSenderEmail());
                
            } catch (MessagingException e) {
                log.error("Failed to send email notification: {}", e.getMessage(), e);
            }
        } else {
            log.warn("No template found for event type: {}", notification.getEventType());
        }
    }

    private String getUserName(String email) {
        try {
            // Try to get user from Keycloak
            return Optional.ofNullable(keycloakService.getUserByEmail(email))
                    .map(user -> user.getFirstName() + " " + user.getLastName())
                    .orElse(email.split("@")[0]); // Fallback to email username
        } catch (Exception e) {
            log.warn("Failed to get user name for {}: {}", email, e.getMessage());
            return email.split("@")[0]; // Fallback to email username
        }
    }

    private String getSenderName(String email) {
        return getUserName(email);
    }

    private String getReceiverName(String email) {
        return getUserName(email);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return "";
        return dateTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss"));
    }

    private EmailTemplateName getTemplateForEventType(String eventType) {
        return switch (eventType) {
            case "TRANSFER_SCHEDULED" -> EmailTemplateName.SCHEDULED_TRANSFER;
            case "TRANSFER_EXECUTED" -> EmailTemplateName.EXECUTED_TRANSFER;
            case "TRANSFER_FAILED" -> EmailTemplateName.FAILED_TRANSFER;
            case "TRANSFER_CANCELLED" -> EmailTemplateName.CANCELLED_TRANSFER;
            default -> null;
        };
    }

    private String getSubjectForEventType(String eventType) {
        return switch (eventType) {
            case "TRANSFER_SCHEDULED" -> "Your Money Transfer Has Been Scheduled";
            case "TRANSFER_EXECUTED" -> "Your Scheduled Money Transfer Has Been Executed";
            case "TRANSFER_FAILED" -> "Your Scheduled Money Transfer Failed";
            case "TRANSFER_CANCELLED" -> "Your Scheduled Money Transfer Has Been Cancelled";
            default -> "Money Transfer Update";
        };
    }
}