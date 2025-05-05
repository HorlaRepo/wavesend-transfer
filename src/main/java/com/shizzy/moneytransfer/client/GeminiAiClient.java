package com.shizzy.moneytransfer.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiAiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    private void logApiDetails() {
        log.info("Gemini API Key: {}",
                geminiApiKey != null ? (geminiApiKey.substring(0, Math.min(5, geminiApiKey.length())) + "...")
                        : "null");
        log.info("Gemini Model: {}", model);
    }

    /**
     * Generates a text response from Gemini AI
     * 
     * @param prompt            The user prompt to send to the AI
     * @param systemInstruction The system instruction to guide the AI
     * @return A mono with the AI's response text
     */
    public Mono<String> generateResponse(String prompt, String systemInstruction) {
        logApiDetails();

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();

        // Create a new content entry with parts
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        // Combine system instruction and prompt if provided
        String fullText = prompt;
        if (systemInstruction != null && !systemInstruction.isEmpty()) {
            fullText = systemInstruction + "\n\n" + prompt;
        }

        // Add the text as a single part
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", fullText);
        parts.add(textPart);

        // Add parts to content
        content.put("parts", parts);
        contents.add(content);

        // Set the contents in the request body
        requestBody.put("contents", contents);

        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" +
                model + ":generateContent?key=" + geminiApiKey;

        try {
            log.debug("Sending request to Gemini API: {}", objectMapper.writeValueAsString(requestBody));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize request for logging", e);
        }

        return webClientBuilder.build()
                .post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(error -> {
                            log.error("Gemini API error: {}", error);
                            return Mono.error(new RuntimeException("Error from Gemini API: " + error));
                        }))
                .bodyToMono(String.class)
                .map(this::extractTextFromResponse)
                .doOnError(e -> log.error("Error calling Gemini API: {}", e.getMessage()))
                .onErrorResume(
                        e -> Mono.just("I'm having trouble communicating with my AI service. Please try again later."));
    }

    /**
     * Generates a structured response from Gemini AI with JSON extraction
     *
     * @param prompt         The user prompt to send to the AI
     * @param expectedFormat Description of the expected JSON format
     * @param targetClass    The class to deserialize the JSON into
     * @return A mono with the deserialized object
     */
    public <T> Mono<T> generateStructuredResponse(String prompt, String expectedFormat, Class<T> targetClass) {
        String structuredPrompt = prompt + "\n\nPlease respond with valid JSON in this format: " + expectedFormat;
        String systemInstruction = "You are a financial assistant API. Extract information from the user message and respond with only valid JSON matching the requested format.";

        return generateResponse(structuredPrompt, systemInstruction)
                .map(response -> {
                    try {
                        // Try to extract JSON from the response if it's wrapped in markdown or text
                        String jsonStr = extractJsonFromText(response);
                        return objectMapper.readValue(jsonStr, targetClass);
                    } catch (Exception e) {
                        log.error("Failed to parse structured response: {}", response, e);
                        throw new RuntimeException("Failed to parse AI response as structured data", e);
                    }
                });
    }

    /**
     * Maintains a multi-turn conversation with the AI
     * 
     * @param conversation      The conversation history
     * @param newUserMessage    The new user message
     * @param systemInstruction The system instruction
     * @return A mono with the AI's response
     */

    public Mono<String> continueConversation(List<ConversationMessage> conversation,
            String newUserMessage,
            String systemInstruction) {
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();

        // If there's a system instruction, add it as context to the first message
        if (systemInstruction != null && !systemInstruction.isEmpty() && !conversation.isEmpty()) {
            // Prepend system instruction to the first message in conversation
            ConversationMessage firstMsg = conversation.get(0);
            String enhancedContent = systemInstruction + "\n\n" + firstMsg.getContent();

            Map<String, Object> contentMap = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();

            Map<String, Object> part = new HashMap<>();
            part.put("text", enhancedContent);
            parts.add(part);

            contentMap.put("parts", parts);
            contents.add(contentMap);

            // Add the rest of the conversation
            for (int i = 1; i < conversation.size(); i++) {
                addSimpleMessageToContents(contents, conversation.get(i).getContent());
            }
        } else {
            // No system instruction or empty conversation,
            // just add all messages as they are
            for (ConversationMessage msg : conversation) {
                addSimpleMessageToContents(contents, msg.getContent());
            }
        }

        // Add new user message
        addSimpleMessageToContents(contents, newUserMessage);

        // Set contents in requestBody
        requestBody.put("contents", contents);

        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" +
                model + ":generateContent?key=" + geminiApiKey;

        return webClientBuilder.build()
                .post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractTextFromResponse)
                .doOnError(e -> log.error("Error calling Gemini API: {}", e.getMessage()))
                .onErrorResume(
                        e -> Mono.just("I'm having trouble communicating with my AI service. Please try again later."));
    }

    private String extractTextFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");

                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }

            log.warn("Unexpected response format from Gemini API: {}", response);
            return "I received an unexpected response format. Please try again.";
        } catch (Exception e) {
            log.error("Error parsing Gemini API response", e);
            return "I had trouble understanding the AI response. Please try again.";
        }
    }

    private String extractJsonFromText(String text) throws JsonProcessingException {
        // Try to extract JSON if it's wrapped in markdown code blocks or text
        if (text.contains("```json") && text.contains("```")) {
            int start = text.indexOf("```json") + 7;
            int end = text.indexOf("```", start);
            return text.substring(start, end).trim();
        } else if (text.contains("```") && text.contains("```")) {
            int start = text.indexOf("```") + 3;
            int end = text.indexOf("```", start);
            return text.substring(start, end).trim();
        } else if (text.startsWith("{") && text.endsWith("}")) {
            // Looks like it's already JSON
            return text;
        } else {
            // Try to find JSON object in the text
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}') + 1;
            if (start >= 0 && end > start) {
                return text.substring(start, end);
            }
        }

        // If we can't extract JSON, throw an exception
        throw new JsonProcessingException("Failed to extract JSON from AI response") {
        };
    }

    private void addSimpleMessageToContents(List<Map<String, Object>> contents, String messageText) {
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> part = new HashMap<>();
        part.put("text", messageText);
        parts.add(part);

        content.put("parts", parts);
        contents.add(content);
    }
}