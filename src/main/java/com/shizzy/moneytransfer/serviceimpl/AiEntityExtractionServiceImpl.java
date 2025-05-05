package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.client.GeminiAiClient;
import com.shizzy.moneytransfer.dto.ScheduledTransferDetails;
import com.shizzy.moneytransfer.dto.TransferDetails;
import com.shizzy.moneytransfer.dto.WithdrawalDetails;
import com.shizzy.moneytransfer.enums.RecurrenceType;
import com.shizzy.moneytransfer.service.AiEntityExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEntityExtractionServiceImpl implements AiEntityExtractionService {

    private final GeminiAiClient geminiAiClient;

    @Override
    public Mono<ScheduledTransferDetails> extractScheduledTransferDetails(String userId, String message) {
        String JSON_FORMAT = "{\n" +
                "  \"amount\": number,\n" +
                "  \"recipientName\": \"string\",\n" +
                "  \"scheduledDateTime\": \"2025-01-01T10:00:00\",\n" +
                "  \"recurrenceType\": \"NONE|DAILY|WEEKLY|MONTHLY\",\n" +
                "  \"recurrenceEndDate\": \"2025-02-01T10:00:00\",\n" +
                "  \"totalOccurrences\": number,\n" +
                "  \"note\": \"string\"\n" +
                "}";

        // Include today's date in the prompt to help the AI understand relative dates
        String prompt = String.format(
                "Extract scheduled money transfer details from this message: \"%s\"\n" +
                        "Today's date is %s. Use this as reference for any relative dates like 'tomorrow', 'next week', etc.",
                message,
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        return geminiAiClient.generateStructuredResponse(prompt, JSON_FORMAT, Map.class)
                .map(responseMap -> {
                    ScheduledTransferDetails details = new ScheduledTransferDetails();

                    // Extract basic fields
                    details.setAmount(parseAmount(responseMap.get("amount")));
                    details.setRecipientName(responseMap.get("recipientName").toString());
                    details.setNote(responseMap.get("note") != null ? responseMap.get("note").toString() : null);

                    // Handle recurrence type if present
                    if (responseMap.get("recurrenceType") != null) {
                        try {
                            String recurrence = responseMap.get("recurrenceType").toString();
                            details.setRecurrenceType(RecurrenceType.valueOf(recurrence));
                        } catch (Exception e) {
                            details.setRecurrenceType(RecurrenceType.NONE);
                            log.warn("Invalid recurrence type, defaulting to NONE: {}", e.getMessage());
                        }
                    } else {
                        details.setRecurrenceType(RecurrenceType.NONE);
                    }

                    // Handle recurrence end date if present
                    if (responseMap.get("recurrenceEndDate") != null) {
                        try {
                            String endDateStr = responseMap.get("recurrenceEndDate").toString();
                            LocalDateTime endDate = LocalDateTime.parse(endDateStr,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                            details.setRecurrenceEndDate(endDate);
                        } catch (Exception e) {
                            log.warn("Failed to parse recurrence end date: {}", e.getMessage());
                        }
                    }

                    // Handle occurrences if present
                    if (responseMap.get("totalOccurrences") != null) {
                        try {
                            details.setTotalOccurrences(
                                    Integer.parseInt(responseMap.get("totalOccurrences").toString()));
                        } catch (Exception e) {
                            log.warn("Failed to parse total occurrences: {}", e.getMessage());
                        }
                    }

                    // Handle scheduled date/time with special care for relative dates
                    LocalDateTime scheduledDateTime = null;
                    if (responseMap.get("scheduledDateTime") != null) {
                        String scheduledDateStr = responseMap.get("scheduledDateTime").toString();

                        try {
                            // First try standard ISO format
                            scheduledDateTime = LocalDateTime.parse(scheduledDateStr,
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                        } catch (Exception e1) {
                            try {
                                // Try alternate format
                                scheduledDateTime = LocalDateTime.parse(scheduledDateStr,
                                        DateTimeFormatter.ISO_DATE_TIME);
                            } catch (Exception e2) {
                                log.warn("Failed to parse scheduled date: {}", e2.getMessage());
                                // Default to tomorrow morning if parsing fails
                                scheduledDateTime = LocalDateTime.now().plusDays(1)
                                        .withHour(9).withMinute(0).withSecond(0).withNano(0);
                            }
                        }
                    } else {
                        // If no date provided, default to tomorrow
                        scheduledDateTime = LocalDateTime.now().plusDays(1)
                                .withHour(9).withMinute(0).withSecond(0).withNano(0);
                    }

                    // CRITICAL FIX: Check message for relative date indicators and override if
                    // needed
                    scheduledDateTime = overrideIncorrectDates(scheduledDateTime, message);

                    // Validate the date is in the future
                    LocalDateTime now = LocalDateTime.now();
                    if (scheduledDateTime.isBefore(now)) {
                        // If it's in the past, move it to tomorrow at the same time
                        scheduledDateTime = now.plusDays(1)
                                .withHour(scheduledDateTime.getHour())
                                .withMinute(scheduledDateTime.getMinute())
                                .withSecond(0).withNano(0);
                    }

                    details.setScheduledDateTime(scheduledDateTime);

                    // Log the final details for debugging
                    log.info("Extracted transfer details: recipient={}, amount={}, date={}, message={}",
                            details.getRecipientName(),
                            details.getAmount(),
                            details.getScheduledDateTime().format(
                                    DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a")),
                            message);

                    return details;
                })
                .onErrorResume(e -> {
                    log.warn("Failed to extract scheduled transfer details using AI, falling back to defaults: {}",
                            e.getMessage());
                    return Mono.just(createDefaultScheduledTransferDetails(message));
                });
    }

    // Helper method for overriding incorrect dates
    private LocalDateTime overrideIncorrectDates(LocalDateTime extractedDate, String originalUserMessage) {
        String lowerMessage = originalUserMessage.toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        // Parse time from message
        int hour = 15; // Default to 3 PM
        int minute = 0;

        // Extract specific time from message if available
        Pattern timePattern = Pattern.compile("(\\d{1,2})(?::(\\d{1,2}))?\\s*(am|pm)", Pattern.CASE_INSENSITIVE);
        Matcher timeMatcher = timePattern.matcher(lowerMessage);

        if (timeMatcher.find()) {
            int parsedHour = Integer.parseInt(timeMatcher.group(1));
            String ampm = timeMatcher.group(3).toLowerCase();

            // Convert to 24-hour format
            if (ampm.equals("pm") && parsedHour < 12) {
                parsedHour += 12;
            } else if (ampm.equals("am") && parsedHour == 12) {
                parsedHour = 0;
            }

            hour = parsedHour;

            // Parse minutes if provided
            if (timeMatcher.group(2) != null) {
                minute = Integer.parseInt(timeMatcher.group(2));
            }
        }

        // Handle specific relative date patterns
        if (lowerMessage.contains("tomorrow")) {
            return now.plusDays(1)
                    .withHour(hour)
                    .withMinute(minute)
                    .withSecond(0)
                    .withNano(0);
        } else if (lowerMessage.contains("today")) {
            return now
                    .withHour(hour)
                    .withMinute(minute)
                    .withSecond(0)
                    .withNano(0);
        } else if (lowerMessage.contains("next week")) {
            return now.plusWeeks(1)
                    .withHour(hour)
                    .withMinute(minute)
                    .withSecond(0)
                    .withNano(0);
        } else if (extractedDate != null) {
            // Keep the AI's date but ensure correct time if specified in message
            if (timeMatcher.find()) {
                return extractedDate
                        .withHour(hour)
                        .withMinute(minute)
                        .withSecond(0)
                        .withNano(0);
            }
        }

        return extractedDate;
    }

    // Helper method to parse amounts from various formats
    private BigDecimal parseAmount(Object amountObj) {
        if (amountObj == null) {
            return new BigDecimal("0.00");
        }

        String amountStr = amountObj.toString().trim();

        // Remove currency symbols and commas
        amountStr = amountStr.replaceAll("[$,]", "");

        try {
            return new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse amount: {}", amountStr);
            return new BigDecimal("0.00");
        }
    }

    // Fallback method for when AI extraction fails
    private ScheduledTransferDetails createDefaultScheduledTransferDetails(String message) {
        ScheduledTransferDetails details = new ScheduledTransferDetails();
        details.setAmount(new BigDecimal("0.00"));
        details.setRecipientName("Unknown Recipient");
        details.setRecurrenceType(RecurrenceType.NONE);

        // Set tomorrow at 9 AM as default time
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1)
                .withHour(9).withMinute(0).withSecond(0).withNano(0);
        details.setScheduledDateTime(tomorrow);

        // Try to extract recipient from message as fallback
        Pattern namePattern = Pattern.compile("to\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)");
        Matcher nameMatcher = namePattern.matcher(message);
        if (nameMatcher.find()) {
            details.setRecipientName(nameMatcher.group(1));
        }

        // Try to extract amount from message as fallback
        Pattern amountPattern = Pattern.compile("\\$?(\\d+(?:\\.\\d+)?)");
        Matcher amountMatcher = amountPattern.matcher(message);
        if (amountMatcher.find()) {
            try {
                details.setAmount(new BigDecimal(amountMatcher.group(1)));
            } catch (NumberFormatException e) {
                // Keep default amount
            }
        }

        return details;
    }

    @Override
    public Mono<TransferDetails> extractTransferDetails(String userId, String message) {
        String JSON_FORMAT = "{\n" +
                "  \"amount\": number,\n" +
                "  \"recipientName\": \"string\",\n" +
                "  \"note\": \"string\"\n" +
                "}";

        // Create a prompt for the AI to extract details
        String prompt = String.format(
                "Extract money transfer details from this message: \"%s\"\n" +
                        "Today's date is %s.",
                message,
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));

        return geminiAiClient.generateStructuredResponse(prompt, JSON_FORMAT, Map.class)
                .map(responseMap -> {
                    TransferDetails details = new TransferDetails();

                    // Extract basic fields
                    details.setAmount(parseAmount(responseMap.get("amount")));
                    details.setRecipientName(responseMap.get("recipientName").toString());
                    details.setNote(responseMap.get("note") != null ? responseMap.get("note").toString() : null);

                    // Log the extracted details
                    log.info("Extracted transfer details: recipient={}, amount={}, message={}",
                            details.getRecipientName(),
                            details.getAmount(),
                            message);

                    return details;
                })
                .onErrorResume(e -> {
                    log.warn("Failed to extract transfer details using AI, falling back to defaults: {}",
                            e.getMessage());
                    return Mono.just(createDefaultTransferDetails(message));
                });
    }

    @Override
    public Mono<WithdrawalDetails> extractWithdrawalDetails(String userId, String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'extractWithdrawalDetails'");
    }

    // Helper method to create default transfer details
    private TransferDetails createDefaultTransferDetails(String message) {
        TransferDetails details = new TransferDetails();
        details.setAmount(new BigDecimal("0.00"));
        details.setRecipientName("Unknown Recipient");

        // Try to extract recipient from message as fallback
        Pattern namePattern = Pattern.compile("to\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)");
        Matcher nameMatcher = namePattern.matcher(message);
        if (nameMatcher.find()) {
            details.setRecipientName(nameMatcher.group(1));
        }

        // Try to extract amount from message as fallback
        Pattern amountPattern = Pattern.compile("\\$?(\\d+(?:\\.\\d+)?)");
        Matcher amountMatcher = amountPattern.matcher(message);
        if (amountMatcher.find()) {
            try {
                details.setAmount(new BigDecimal(amountMatcher.group(1)));
            } catch (NumberFormatException e) {
                // Keep default amount
            }
        }

        return details;
    }
}