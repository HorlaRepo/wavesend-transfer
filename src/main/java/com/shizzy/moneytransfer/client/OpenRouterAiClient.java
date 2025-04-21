package com.shizzy.moneytransfer.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenRouterAiClient {

    private final WebClient.Builder webClientBuilder;
    // Create a default ObjectMapper instead of injecting it
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    @Value("${openrouter.api-key}")
    private String openRouterApiKey;

    @Value("${openrouter.api-url}")
    private String openRouterApiUrl;

    @Value("${openrouter.model}")
    private String model;

    @Value("${openrouter.max-tokens}")
    private int maxTokens;

    @Value("${openrouter.temperature}")
    private double temperature;

    // List of AI personas to use for varied suggestions
    private final List<String> aiPersonas = List.of(
            "You are a helpful financial assistant who uses friendly, casual language.",
            "You are a witty friend who cares about people's financial well-being.",
            "You are a supportive ally who notices when friends might need help.",
            "You are a playful financial buddy who uses emoji and slang occasionally.",
            "You are a wise mentor who encourages generosity in a lighthearted way.",
            "You are a clever friend who spots opportunities to help others.");

    // Different prompt templates for variety
    private final List<String> promptTemplates = List.of(
            // Friendship-focused
            "Create a friendly suggestion for {userName} to help out {beneficiaryName} who seems low on cash " +
                    "(balance: ${beneficiaryBalance}). {userName} has ${userBalance} available. Suggest a specific " +
                    "amount between $5-$20 and a casual reason to help. Keep it brief and friendly.",

            // Opportunity-focused
            "Write a quick note to {userName} suggesting they might want to send some money " +
                    "to {beneficiaryName} who's running low (just ${beneficiaryBalance} left). {userName} has " +
                    "${userBalance} so they can afford to help. Suggest a specific amount and a light-hearted reason why.",

            // Memory-focused
            "Write as if {userName} and {beneficiaryName} are friends with shared experiences. " +
                    "Create a brief suggestion for {userName} (who has ${userBalance}) to send some money to " +
                    "{beneficiaryName} who's down to ${beneficiaryBalance}. Use an imagined shared memory as " +
                    "context and suggest a specific amount between $5-$20.",

            // Need-focused
            "Create a casual heads-up for {userName} that their {relationship} {beneficiaryName} " +
                    "is running low on funds (${beneficiaryBalance}). Suggest they send a specific amount " +
                    "(between $5-$20, considering {userName}'s ${userBalance} balance) and mention " +
                    "a creative reason why {beneficiaryName} might need it right now.",

            // Celebration-focused
            "Write a brief, upbeat suggestion for {userName}to brighten {beneficiaryName}'s day with " +
                    "a money transfer. {beneficiaryName} has a low balance (${beneficiaryBalance}) while " +
                    "{userName} has ${userBalance}. Suggest a specific amount they could send and frame it " +
                    "as a surprise or celebration.");

    // Fallback suggestions with variety
    private final List<String> defaultSuggestions = List.of(
            "Looks like {name} is running a little low on funds. Maybe send them $10 to help out? That's what friends are for! 💸",
            "Hey! {name} might appreciate a little financial boost right now. How about sending $15 their way? ✨",
            "{name}'s wallet is looking light these days. A quick $12 transfer could make their day! 🌟",
            "Friend alert! {name} could use some cash. Why not send $8 for coffee and snacks? ☕️",
            "Seems like {name} might be counting pennies at the moment. A $20 surprise could really help them out! 💯",
            "Your buddy {name} appears to be low on funds. Consider sending $10 - small gestures make big differences! 🙌",
            "Notice {name}'s balance is looking slim? A quick $15 transfer could save the day! 🦸‍♂️");

    public Mono<String> generateBeneficiarySuggestion(
            String userName, String beneficiaryName, BigDecimal userBalance,
            BigDecimal beneficiaryBalance, String relationship) {

        WebClient client = webClientBuilder
                .baseUrl(openRouterApiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openRouterApiKey)
                .defaultHeader("HTTP-Referer", "https://wavesend.cc") // Your app domain
                .build();

        // Select a random prompt template and persona for variety
        String promptTemplate = promptTemplates.get(random.nextInt(promptTemplates.size()));
        String persona = aiPersonas.get(random.nextInt(aiPersonas.size()));

        // Add contextual enrichment based on day of week, time, etc
        Map<String, String> contextParams = enrichWithContext();

        // Format the selected template with user data and context
        String prompt = formatPrompt(promptTemplate, userName, beneficiaryName,
                userBalance, beneficiaryBalance, relationship, contextParams);

        // Add additional personality to system message
        String systemMessage = persona + " " + getAdditionalInstructions(contextParams);

        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(new Message("system",
                        systemMessage),
                        new Message("user", prompt)),
                Math.min(temperature + 0.1, 0.9),
                maxTokens);

        try {
            log.debug("Sending request to OpenRouter API: {}", objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize request for logging", e);
        }

        return client.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractResponseText)
                .doOnError(e -> log.error("Error calling OpenRouter API", e))
                .onErrorResume(e -> Mono.just(getRandomDefaultSuggestion(beneficiaryName)));
    }

    private String extractResponseText(Map<String, Object> response) {
        try {
            log.debug("Received response from OpenRouter API: {}",
                    objectMapper.writeValueAsString(response));

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
            return getDefaultSuggestion("your friend");
        } catch (Exception e) {
            log.error("Error parsing OpenRouter API response", e);
            return getDefaultSuggestion("your friend");
        }
    }


    private String getDefaultSuggestion(String name) {
        return "Looks like " + name + " is running a little low on funds. " +
                "Maybe send them $10 to help out? That's what friends are for! 💸";
    }

    private String formatPrompt(String template, String userName, String beneficiaryName,
            BigDecimal userBalance, BigDecimal beneficiaryBalance,
            String relationship, Map<String, String> contextParams) {
        String result = template
                .replace("{userName}", userName)
                .replace("{beneficiaryName}", beneficiaryName)
                .replace("{userBalance}", userBalance.toString())
                .replace("{beneficiaryBalance}", beneficiaryBalance.toString())
                .replace("{relationship}", relationship);

        // Add any additional context parameters
        for (Map.Entry<String, String> entry : contextParams.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return result;
    }

    private Map<String, String> enrichWithContext() {
        Map<String, String> context = new HashMap<>();

        // Add day of week context
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        context.put("dayOfWeek", day.getDisplayName(TextStyle.FULL, Locale.getDefault()));

        // Is it weekend?
        boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        context.put("isWeekend", String.valueOf(isWeekend));

        // Time of day
        int hour = LocalTime.now().getHour();
        String timeOfDay = (hour >= 5 && hour < 12) ? "morning"
                : (hour >= 12 && hour < 17) ? "afternoon" : (hour >= 17 && hour < 22) ? "evening" : "night";
        context.put("timeOfDay", timeOfDay);

        // Is it near payday? (assuming end of month)
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        int lastDayOfMonth = LocalDate.now().lengthOfMonth();
        boolean nearPayday = dayOfMonth >= lastDayOfMonth - 5 || dayOfMonth <= 3;
        context.put("nearPayday", String.valueOf(nearPayday));

        // Season
        Month month = LocalDate.now().getMonth();
        String season = switch (month) {
            case DECEMBER, JANUARY, FEBRUARY -> "winter";
            case MARCH, APRIL, MAY -> "spring";
            case JUNE, JULY, AUGUST -> "summer";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "fall";
            default -> "current season";
        };
        context.put("season", season);

        // Special occasions/holidays
        Optional<String> holiday = checkForHoliday();
        holiday.ifPresent(h -> context.put("holiday", h));

        return context;
    }

    private Optional<String> checkForHoliday() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        // Very simple holiday detection - expand as needed
        if (month == 12 && day >= 15 && day <= 31)
            return Optional.of("Christmas");
        if (month == 1 && day == 1)
            return Optional.of("New Year's Day");
        if (month == 2 && day == 14)
            return Optional.of("Valentine's Day");
        if (month == 10 && day == 31)
            return Optional.of("Halloween");
        if (month == 11 && (day >= 22 && day <= 28) && today.getDayOfWeek() == DayOfWeek.THURSDAY)
            return Optional.of("Thanksgiving");

        return Optional.empty();
    }

    private String getAdditionalInstructions(Map<String, String> context) {
        StringBuilder instructions = new StringBuilder();

        instructions.append("Keep responses brief, under 80 words. ");

        // Add holiday-specific instructions
        if (context.containsKey("holiday")) {
            instructions.append("It's currently around ").append(context.get("holiday"))
                    .append(", so you could reference that occasion. ");
        }

        // Weekend vs weekday tone
        if ("true".equals(context.get("isWeekend"))) {
            instructions.append("It's the weekend, so a relaxed, fun tone is appropriate. ");
        } else {
            instructions.append("It's a weekday, so a helpful, supportive tone works well. ");
        }

        // Time of day adjustments
        instructions.append("Since it's ").append(context.get("timeOfDay"))
                .append(", adjust your tone accordingly. ");

        return instructions.toString();
    }

    private String getRandomDefaultSuggestion(String name) {
        String template = defaultSuggestions.get(random.nextInt(defaultSuggestions.size()));
        return template.replace("{name}", name);
    }

    
    @Data
    @AllArgsConstructor
    private static class ChatCompletionRequest {
        private String model;
        private List<Message> messages;
        private double temperature;
        @JsonProperty("max_tokens")
        private int maxTokens;
    }

    @Data
    @AllArgsConstructor
    private static class Message {
        private String role;
        private String content;
    }
}