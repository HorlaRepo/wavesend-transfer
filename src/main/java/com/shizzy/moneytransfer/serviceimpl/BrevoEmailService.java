package com.shizzy.moneytransfer.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shizzy.moneytransfer.dto.ScheduledTransferEmailDto;
import com.shizzy.moneytransfer.dto.TransferEmailDto;
import com.shizzy.moneytransfer.enums.EmailTemplateName;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.service.EmailService;
import com.shizzy.moneytransfer.service.KeycloakService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrevoEmailService implements EmailService {

    private final KeycloakService keycloakService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${brevo.api-key}")
    private String apiKey;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final String BREVO_API_URL = "https://api.sendinblue.com/v3/smtp/email";

    /**
     * Reads an HTML template from the resources directory
     * @param templatePath path to the template file (e.g., "templates/deposit_success.html")
     * @return the template content as a string
     */
    private String readEmailTemplate(String templatePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(templatePath);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     * Replaces all placeholders in the template with values from the properties map
     * @param template the HTML template string
     * @param properties a map of key-value pairs to replace in the template
     * @return the processed HTML string
     */
    private String processTemplate(String template, Map<String, Object> properties) {
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = properties.get(key);
            String replacement = (value != null) ? value.toString() : "";
            // Handle conditional logic for status badges
            if (key.equals("status") && properties.containsKey("status")) {
                // The template handles this with conditional logic
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public void sendEmail(String to, Map<String, Object> properties, EmailTemplateName templateName, String subject) throws MessagingException {
        try {
            String templatePath = getTemplatePathForEmailType(templateName);
            String htmlTemplate = readEmailTemplate(templatePath);
            String processedHtml = processTemplate(htmlTemplate, properties);
            
            // Create request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sender", Map.of(
                "name", "WaveSend",
                "email", "no-reply@wavesend.cc"
            ));
            requestBody.put("to", new Object[] {
                Map.of("email", to)
            });
            requestBody.put("subject", subject);
            requestBody.put("htmlContent", processedHtml);
            
            // Set up HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            
            // Create HTTP entity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Send the request
            ResponseEntity<Map> response = restTemplate.exchange(
                BREVO_API_URL,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent to {} using local HTML template. Message ID: {}", 
                         to, response.getBody() != null ? response.getBody().get("messageId") : "unknown");
                log.debug("Email parameters: {}", objectMapper.writeValueAsString(properties));
            } else {
                log.error("Failed to send email via Brevo API: Status code {}", response.getStatusCode());
                throw new MessagingException("Failed to send email: HTTP error " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Failed to send email via Brevo API: {}", e.getMessage(), e);
            throw new MessagingException("Failed to send email: " + e.getMessage());
        } catch (IOException e) {
            log.error("Failed to read email template: {}", e.getMessage(), e);
            throw new MessagingException("Failed to read email template: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send email via Brevo: {}", e.getMessage(), e);
            throw new MessagingException("Failed to send email: " + e.getMessage());
        }
    }

    private String getTemplatePathForEmailType(EmailTemplateName templateName) {
        return switch (templateName) {
            case DEPOSIT -> "templates/deposit_success.html";
            case WITHDRAWAL -> "templates/withdrawal_success.html";
            case CREDIT_TRANSFER -> "templates/credit_transfer.html";
            case DEBIT_TRANSFER -> "templates/debit_transfer.html";
            case SCHEDULED_TRANSFER -> "templates/scheduled_transfer.html";
            case EXECUTED_TRANSFER -> "templates/executed_transfer.html";
            case CANCELLED_TRANSFER -> "templates/cancelled_transfer.html";
            case FAILED_TRANSFER -> "templates/failed_transfer.html";
            case OTP_VERIFICATION -> "templates/verification.html";
        };
    }

    @Override
    public void sendEmail() {
        // Default implementation not used for Brevo
        throw new UnsupportedOperationException("Default sendEmail method not implemented for Brevo");
    }

    @Override
    @Async
    public void sendCreditTransactionEmail(Transaction transaction, String userEmail, String name, String subject) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("company_logo", "https://wavesend.com/logo.png"); // Replace with your actual logo URL
            data.put("recipient_name", name);
            data.put("currency_symbol", getCurrencySymbol(transaction.getWallet()));
            data.put("amount", transaction.getAmount().toString());
            data.put("reference_number", transaction.getReferenceNumber());
            data.put("transaction_date", formatTransactionDate(transaction.getTransactionDate()));
            data.put("description", transaction.getDescription());
            data.put("status", transaction.getCurrentStatus());
            data.put("new_balance", transaction.getWallet().getBalance().toString());
            data.put("dashboard_url", "http://localhost:4200/account/transactions");
            data.put("facebook_url", "https://facebook.com/wavesend");
            data.put("twitter_url", "https://twitter.com/wavesend");
            data.put("instagram_url", "https://instagram.com/wavesend");
            data.put("current_year", String.valueOf(LocalDateTime.now().getYear()));
            data.put("source", transaction.getSource() != null ? transaction.getSource() : "Wallet");
            
            sendEmail(userEmail, data, EmailTemplateName.DEPOSIT, subject);
            log.info("Deposit email sent to {}", userEmail);
            // Log the full parameters map for detailed debugging
            try {
                log.info("Credit transaction email parameters: {}", objectMapper.writeValueAsString(data));
            } catch (Exception e) {
                log.warn("Could not serialize email parameters: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send deposit email notification: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendDebitTransactionEmail(Transaction transaction, String userEmail, String name, String subject) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("company_logo", "https://wavesend.cc/logo.png"); // Replace with your actual logo URL
            params.put("recipient_name", name);
            params.put("currency_symbol", getCurrencySymbol(transaction.getWallet()));
            params.put("amount", transaction.getAmount().toString());
            params.put("reference_number", transaction.getReferenceNumber());
            params.put("transaction_date", formatTransactionDate(transaction.getTransactionDate()));
            params.put("description", transaction.getDescription());
            params.put("status", transaction.getCurrentStatus());
            params.put("new_balance", transaction.getWallet().getBalance().toString());
            params.put("dashboard_url", "http://localhost:4200/account/transactions");
            params.put("facebook_url", "https://facebook.com/wavesend");
            params.put("twitter_url", "https://twitter.com/wavesend");
            params.put("instagram_url", "https://instagram.com/wavesend");
            params.put("current_year", String.valueOf(LocalDateTime.now().getYear()));
            params.put("source", transaction.getSource() != null ? transaction.getSource() : "Wallet");
            
            // Note: For now this will try to use withdrawal_success.html which doesn't exist yet
            // You may want to temporarily use deposit_success.html for both until you create the withdrawal template
            try {
                sendEmail(userEmail, params, EmailTemplateName.WITHDRAWAL, subject);
            } catch (MessagingException e) {
                // If withdrawal template doesn't exist, fall back to deposit template
                if (e.getMessage().contains("withdrawal_success.html")) {
                    log.warn("Withdrawal template not found, falling back to deposit template");
                    sendEmail(userEmail, params, EmailTemplateName.DEPOSIT, subject);
                } else {
                    throw e;
                }
            }
            
            log.info("Withdrawal email sent to {}", userEmail);
            // Log the full parameters map for detailed debugging
            try {
                log.info("Debit transaction email parameters: {}", objectMapper.writeValueAsString(params));
            } catch (Exception e) {
                log.warn("Could not serialize email parameters: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send withdrawal email notification: {}", e.getMessage(), e);
        }
    }

    
    @Override
    @Async
    public void sendCreditTransferEmail(TransferEmailDto emailDto) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("company_logo", "https://wavesend.cc/logo.png");
            params.put("receiver_name", emailDto.getReceiverName());
            params.put("sender_name", emailDto.getSenderName());
            params.put("sender_email", emailDto.getSenderEmail());
            params.put("receiver_email", emailDto.getReceiverEmail());
            params.put("currency_symbol", getCurrencySymbol(emailDto.getTransaction().getWallet()));
            params.put("amount", emailDto.getTransaction().getAmount().toString());
            params.put("reference_number", emailDto.getTransaction().getReferenceNumber());
            params.put("transaction_date", formatTransactionDate(emailDto.getTransaction().getTransactionDate()));
            params.put("description", emailDto.getTransaction().getDescription());
            params.put("status", emailDto.getTransaction().getCurrentStatus());
            params.put("new_balance", emailDto.getTransaction().getWallet().getBalance().toString());
            params.put("dashboard_url", "http://localhost:4200/account/transactions");
            params.put("facebook_url", "https://facebook.com/wavesend");
            params.put("twitter_url", "https://twitter.com/wavesend");
            params.put("instagram_url", "https://instagram.com/wavesend");
            params.put("current_year", String.valueOf(LocalDateTime.now().getYear()));
            
            // Send to receiver
            String recipientEmail = emailDto.getRecipientEmail(true);
            sendEmail(recipientEmail, params, EmailTemplateName.CREDIT_TRANSFER, emailDto.getSubject());
            log.info("Credit transfer email sent to {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send credit transfer email notification: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendDebitTransferEmail(TransferEmailDto emailDto) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("company_logo", "https://wavesend.cc/logo.png");
            params.put("sender_name", emailDto.getSenderName());
            params.put("receiver_name", emailDto.getReceiverName());
            params.put("sender_email", emailDto.getSenderEmail());
            params.put("receiver_email", emailDto.getReceiverEmail());
            params.put("currency_symbol", getCurrencySymbol(emailDto.getTransaction().getWallet()));
            params.put("amount", emailDto.getTransaction().getAmount().toString());
            params.put("reference_number", emailDto.getTransaction().getReferenceNumber());
            params.put("transaction_date", formatTransactionDate(emailDto.getTransaction().getTransactionDate()));
            params.put("description", emailDto.getTransaction().getDescription());
            params.put("status", emailDto.getTransaction().getCurrentStatus());
            params.put("new_balance", emailDto.getTransaction().getWallet().getBalance().toString());
            params.put("dashboard_url", "http://localhost:4200/account/transactions");
            params.put("facebook_url", "https://facebook.com/wavesend");
            params.put("twitter_url", "https://twitter.com/wavesend");
            params.put("instagram_url", "https://instagram.com/wavesend");
            params.put("current_year", String.valueOf(LocalDateTime.now().getYear()));
            
            // Send to sender
            String recipientEmail = emailDto.getRecipientEmail(false);
            sendEmail(recipientEmail, params, EmailTemplateName.DEBIT_TRANSFER, emailDto.getSubject());
            log.info("Debit transfer email sent to {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send debit transfer email notification: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void sendScheduledTransferEmail(ScheduledTransferEmailDto emailDto) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("company_logo", "https://wavesend.cc/logo.png");
            params.put("sender_name", emailDto.getSenderName());
            params.put("receiver_name", emailDto.getReceiverName());
            params.put("receiver_email", emailDto.getReceiverEmail());
            params.put("currency_symbol", "$"); // Default currency symbol
            params.put("amount", emailDto.getAmount().toString());
            params.put("transfer_id", emailDto.getTransferId().toString());
            params.put("scheduled_date", formatDateTime(emailDto.getScheduledDateTime()));
            params.put("description", emailDto.getDescription());
            params.put("is_recurring", emailDto.isRecurring());
            
            if (emailDto.isRecurring() && emailDto.getRecurrenceType() != null) {
                params.put("recurrence_type", emailDto.getRecurrenceType().toString());
                if (emailDto.getRecurrenceEndDate() != null) {
                    params.put("recurrence_end", formatDateTime(emailDto.getRecurrenceEndDate()));
                } else if (emailDto.getTotalOccurrences() != null) {
                    params.put("recurrence_end", "After " + emailDto.getTotalOccurrences() + " occurrences");
                }
            }
            
            // Dashboard and social media links
            params.put("dashboard_url", "http://localhost:4200/account/scheduled-transfers");
            params.put("facebook_url", "https://facebook.com/wavesend");
            params.put("twitter_url", "https://twitter.com/wavesend");
            params.put("instagram_url", "https://instagram.com/wavesend");
            params.put("current_year", String.valueOf(LocalDateTime.now().getYear()));
            
            sendEmail(emailDto.getSenderEmail(), params, EmailTemplateName.SCHEDULED_TRANSFER, emailDto.getSubject());
            log.info("Scheduled transfer email sent to {}", emailDto.getSenderEmail());
        } catch (Exception e) {
            log.error("Failed to send scheduled transfer email notification: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void sendExecutedTransferEmail(ScheduledTransferEmailDto emailDto) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("company_logo", "https://wavesend.cc/logo.png");
            params.put("sender_name", emailDto.getSenderName());
            params.put("receiver_name", emailDto.getReceiverName());
            params.put("receiver_email", emailDto.getReceiverEmail());
            params.put("currency_symbol", "$"); // Default currency symbol
            params.put("amount", emailDto.getAmount().toString());
            params.put("transfer_id", emailDto.getTransferId().toString());
            params.put("scheduled_date", formatDateTime(emailDto.getScheduledDateTime()));
            params.put("execution_date", formatDateTime(emailDto.getExecutionDateTime() != null ? 
                                                         emailDto.getExecutionDateTime() : 
                                                         LocalDateTime.now()));
            params.put("description", emailDto.getDescription());
            params.put("is_recurring", emailDto.isRecurring());
            
            if (emailDto.isRecurring()) {
                params.put("recurrence_type", emailDto.getRecurrenceType() != null ? 
                                               emailDto.getRecurrenceType().toString() : "");
                if (emailDto.getNextScheduledDate() != null) {
                    params.put("next_scheduled_date", formatDateTime(emailDto.getNextScheduledDate()));
                }
            }
            
            // Dashboard and social media links
            params.put("dashboard_url", "http://localhost:4200/account/transactions");
            params.put("facebook_url", "https://facebook.com/wavesend");
            params.put("twitter_url", "https://twitter.com/wavesend");
            params.put("instagram_url", "https://instagram.com/wavesend");
            params.put("current_year", String.valueOf(LocalDateTime.now().getYear()));
            
            sendEmail(emailDto.getSenderEmail(), params, EmailTemplateName.EXECUTED_TRANSFER, emailDto.getSubject());
            log.info("Executed transfer email sent to {}", emailDto.getSenderEmail());
        } catch (Exception e) {
            log.error("Failed to send executed transfer email notification: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void sendCancelledTransferEmail(ScheduledTransferEmailDto emailDto) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("company_logo", "https://wavesend.cc/logo.png");
            params.put("sender_name", emailDto.getSenderName());
            params.put("receiver_name", emailDto.getReceiverName());
            params.put("receiver_email", emailDto.getReceiverEmail());
            params.put("currency_symbol", "$"); // Default currency symbol
            params.put("amount", emailDto.getAmount().toString());
            params.put("transfer_id", emailDto.getTransferId().toString());
            params.put("scheduled_date", formatDateTime(emailDto.getScheduledDateTime()));
            params.put("cancellation_date", formatDateTime(emailDto.getCancellationDateTime() != null ? 
                                                            emailDto.getCancellationDateTime() : 
                                                            LocalDateTime.now()));
            params.put("description", emailDto.getDescription());
            params.put("is_recurring", emailDto.isRecurring());
            
            if (emailDto.isRecurring() && emailDto.getRecurrenceType() != null) {
                params.put("recurrence_type", emailDto.getRecurrenceType().toString());
            }
            
            // Dashboard and social media links
            params.put("dashboard_url", "http://localhost:4200/account/scheduled-transfers");
            params.put("facebook_url", "https://facebook.com/wavesend");
            params.put("twitter_url", "https://twitter.com/wavesend");
            params.put("instagram_url", "https://instagram.com/wavesend");
            params.put("current_year", String.valueOf(LocalDateTime.now().getYear()));
            
            sendEmail(emailDto.getSenderEmail(), params, EmailTemplateName.CANCELLED_TRANSFER, emailDto.getSubject());
            log.info("Cancelled transfer email sent to {}", emailDto.getSenderEmail());
        } catch (Exception e) {
            log.error("Failed to send cancelled transfer email notification: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @Async
    public void sendFailedTransferEmail(ScheduledTransferEmailDto emailDto) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("company_logo", "https://wavesend.cc/logo.png");
            params.put("sender_name", emailDto.getSenderName());
            params.put("receiver_name", emailDto.getReceiverName());
            params.put("receiver_email", emailDto.getReceiverEmail());
            params.put("currency_symbol", "$"); // Default currency symbol
            params.put("amount", emailDto.getAmount().toString());
            params.put("transfer_id", emailDto.getTransferId().toString());
            params.put("scheduled_date", formatDateTime(emailDto.getScheduledDateTime()));
            params.put("failure_date", formatDateTime(emailDto.getFailureDateTime() != null ? 
                                                      emailDto.getFailureDateTime() : 
                                                      LocalDateTime.now()));
            params.put("description", emailDto.getDescription());
            
            // Dashboard and social media links
            params.put("dashboard_url", "http://localhost:4200/account/scheduled-transfers");
            params.put("facebook_url", "https://facebook.com/wavesend");
            params.put("twitter_url", "https://twitter.com/wavesend");
            params.put("instagram_url", "https://instagram.com/wavesend");
            params.put("current_year", String.valueOf(LocalDateTime.now().getYear()));
            
            sendEmail(emailDto.getSenderEmail(), params, EmailTemplateName.FAILED_TRANSFER, emailDto.getSubject());
            log.info("Failed transfer email sent to {}", emailDto.getSenderEmail());
        } catch (Exception e) {
            log.error("Failed to send failed transfer email notification: {}", e.getMessage(), e);
        }
    }
    
    // Helper method to format dates consistently
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "";
    }
    
    private String formatTransactionDate(LocalDateTime date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }
    
    private String getCurrencySymbol(Wallet wallet) {
        // Default to $ if currency can't be determined
        return "$";
        // If your wallet has currency info, you can implement appropriate logic here
        // e.g., return wallet.getCurrency() != null ? getCurrencySymbolFromCode(wallet.getCurrency()) : "$";
    }
}