package com.shizzy.moneytransfer.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shizzy.moneytransfer.dto.ScheduledTransferEmailDto;
import com.shizzy.moneytransfer.dto.TransferEmailDto;
import com.shizzy.moneytransfer.enums.EmailTemplateName;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
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

/**
 * Mailtrap email service implementation using REST API
 * Replaces Brevo for email sending with improved reliability and deliverability
 *
 * Best practices implemented:
 * - Uses RestTemplate for HTTP communication
 * - Proper error handling and logging
 * - Template-based emails with placeholder replacement
 * - @Async for non-blocking email operations
 * - @Primary to make this the default EmailService implementation
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class MailtrapEmailService implements EmailService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${mailtrap.api-token}")
    private String apiToken;

    @Value("${mailtrap.sender-email:wavesend@demomailtrap.com}")
    private String senderEmail;

    @Value("${mailtrap.sender-name:WaveSend}")
    private String senderName;

    private static final String MAILTRAP_API_URL = "https://send.api.mailtrap.io/api/send";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private String readEmailTemplate(String templatePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(templatePath);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private String processTemplate(String template, Map<String, Object> properties) {
        template = processConditionalBlocks(template, properties);

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = properties.get(key);
            String replacement = (value != null) ? value.toString() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String processConditionalBlocks(String template, Map<String, Object> properties) {
        Pattern ifPattern = Pattern.compile("\\{\\{#if\\s+(\\w+)\\}\\}(.*?)\\{\\{/if\\}\\}", Pattern.DOTALL);
        Matcher matcher = ifPattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String content = matcher.group(2);
            Object value = properties.get(key);

            boolean isTruthy = value != null
                    && !value.toString().isEmpty()
                    && !value.toString().equals("false")
                    && !value.toString().equals("0");

            if (isTruthy) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(content));
            } else {
                matcher.appendReplacement(sb, "");
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

            // Build Mailtrap API request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", Map.of("email", senderEmail, "name", senderName));
            requestBody.put("to", new Object[]{Map.of("email", to)});
            requestBody.put("subject", subject);
            requestBody.put("html", processedHtml);

            // Set up HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiToken);

            // Create HTTP entity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Send the request
            ResponseEntity<Map> response = restTemplate.exchange(
                    MAILTRAP_API_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent to {} using Mailtrap. Response: {}", to,
                        response.getBody() != null ? objectMapper.writeValueAsString(response.getBody()) : "null");
                log.debug("Email parameters: {}", objectMapper.writeValueAsString(properties));
            } else {
                log.error("Failed to send email via Mailtrap API: Status code {}", response.getStatusCode());
                throw new MessagingException("Failed to send email: HTTP error " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Failed to send email via Mailtrap API: {}", e.getMessage(), e);
            throw new MessagingException("Failed to send email: " + e.getMessage());
        } catch (IOException e) {
            log.error("Failed to read email template: {}", e.getMessage(), e);
            throw new MessagingException("Failed to read email template: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email via Mailtrap: {}", e.getMessage(), e);
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
        throw new UnsupportedOperationException("Default sendEmail method not implemented for Mailtrap");
    }

    @Override
    @Async
    public void sendCreditTransactionEmail(Transaction transaction, String userEmail, String name, String subject) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("company_logo", "https://wavesend.cc/logo.png");
            data.put("recipient_name", name);
            data.put("currency_symbol", getCurrencySymbol(transaction.getWallet()));
            data.put("amount", transaction.getAmount().toString());
            data.put("reference_number", transaction.getReferenceNumber());
            data.put("transaction_date", formatTransactionDate(transaction.getTransactionDate()));
            data.put("description", transaction.getDescription());
            data.put("status", transaction.getCurrentStatus());
            data.put("new_balance", transaction.getWallet().getBalance().toString());
            data.put("dashboard_url", "https://app.wavesend.cc/account/transactions");
            data.put("facebook_url", "https://facebook.com/wavesend");
            data.put("twitter_url", "https://twitter.com/wavesend");
            data.put("instagram_url", "https://instagram.com/wavesend");
            data.put("current_year", String.valueOf(LocalDateTime.now().getYear()));
            data.put("source", transaction.getSource() != null ? transaction.getSource() : "Wallet");

            sendEmail(userEmail, data, EmailTemplateName.DEPOSIT, subject);
            log.info("Credit transaction email sent to {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to send credit transaction email notification: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendDebitTransactionEmail(Transaction transaction, String userEmail, String name, String subject) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("company_logo", "https://wavesend.cc/logo.png");
            params.put("recipient_name", name);
            params.put("currency_symbol", getCurrencySymbol(transaction.getWallet()));
            params.put("amount", transaction.getAmount().toString());
            params.put("reference_number", transaction.getReferenceNumber());
            params.put("transaction_date", formatTransactionDate(transaction.getTransactionDate()));
            params.put("description", transaction.getDescription());
            params.put("status", transaction.getCurrentStatus());
            params.put("new_balance", transaction.getWallet().getBalance().toString());
            params.put("dashboard_url", "https://app.wavesend.cc/account/transactions");
            params.put("facebook_url", "https://facebook.com/wavesend");
            params.put("twitter_url", "https://twitter.com/wavesend");
            params.put("instagram_url", "https://instagram.com/wavesend");
            params.put("current_year", String.valueOf(LocalDateTime.now().getYear()));
            params.put("source", transaction.getSource() != null ? transaction.getSource() : "Wallet");

            try {
                sendEmail(userEmail, params, EmailTemplateName.WITHDRAWAL, subject);
            } catch (MessagingException e) {
                if (e.getMessage().contains("withdrawal_success.html")) {
                    log.warn("Withdrawal template not found, falling back to deposit template");
                    sendEmail(userEmail, params, EmailTemplateName.DEPOSIT, subject);
                } else {
                    throw e;
                }
            }

            log.info("Debit transaction email sent to {}", userEmail);
        } catch (Exception e) {
            log.error("Failed to send debit transaction email notification: {}", e.getMessage(), e);
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
            params.put("dashboard_url", "https://app.wavesend.cc/account/transactions");
            params.put("facebook_url", "https://facebook.com/wavesend");
            params.put("twitter_url", "https://twitter.com/wavesend");
            params.put("instagram_url", "https://instagram.com/wavesend");
            params.put("current_year", String.valueOf(LocalDateTime.now().getYear()));

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
            params.put("dashboard_url", "https://app.wavesend.cc/account/transactions");
            params.put("facebook_url", "https://facebook.com/wavesend");
            params.put("twitter_url", "https://twitter.com/wavesend");
            params.put("instagram_url", "https://instagram.com/wavesend");
            params.put("current_year", String.valueOf(LocalDateTime.now().getYear()));

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
            Map<String, Object> params = buildScheduledTransferParams(emailDto);
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
            Map<String, Object> params = buildScheduledTransferParams(emailDto);
            params.put("execution_date", formatDateTime(emailDto.getExecutionDateTime() != null ?
                                                         emailDto.getExecutionDateTime() :
                                                         LocalDateTime.now()));

            if (emailDto.isRecurring() && emailDto.getNextScheduledDate() != null) {
                params.put("next_scheduled_date", formatDateTime(emailDto.getNextScheduledDate()));
            }

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
            Map<String, Object> params = buildScheduledTransferParams(emailDto);
            params.put("cancellation_date", formatDateTime(emailDto.getCancellationDateTime() != null ?
                                                            emailDto.getCancellationDateTime() :
                                                            LocalDateTime.now()));

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
            Map<String, Object> params = buildScheduledTransferParams(emailDto);
            params.put("failure_date", formatDateTime(emailDto.getFailureDateTime() != null ?
                                                      emailDto.getFailureDateTime() :
                                                      LocalDateTime.now()));

            sendEmail(emailDto.getSenderEmail(), params, EmailTemplateName.FAILED_TRANSFER, emailDto.getSubject());
            log.info("Failed transfer email sent to {}", emailDto.getSenderEmail());
        } catch (Exception e) {
            log.error("Failed to send failed transfer email notification: {}", e.getMessage(), e);
        }
    }

    private Map<String, Object> buildScheduledTransferParams(ScheduledTransferEmailDto emailDto) {
        Map<String, Object> params = new HashMap<>();
        params.put("company_logo", "https://wavesend.cc/logo.png");
        params.put("sender_name", emailDto.getSenderName());
        params.put("receiver_name", emailDto.getReceiverName());
        params.put("receiver_email", emailDto.getReceiverEmail());
        params.put("currency_symbol", "$");
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

        params.put("dashboard_url", "https://app.wavesend.cc/account/scheduled-transfers");
        params.put("facebook_url", "https://facebook.com/wavesend");
        params.put("twitter_url", "https://twitter.com/wavesend");
        params.put("instagram_url", "https://instagram.com/wavesend");
        params.put("current_year", String.valueOf(LocalDateTime.now().getYear()));

        return params;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : "";
    }

    private String formatTransactionDate(LocalDateTime date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private String getCurrencySymbol(Wallet wallet) {
        return "$";
    }
}

