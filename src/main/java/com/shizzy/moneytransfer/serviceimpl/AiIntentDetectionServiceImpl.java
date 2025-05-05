package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.client.GeminiAiClient;
import com.shizzy.moneytransfer.dto.TransactionIntent;
import com.shizzy.moneytransfer.service.AiIntentDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiIntentDetectionServiceImpl implements AiIntentDetectionService {

    private final GeminiAiClient geminiAiClient;
    
    @Override
    public Mono<TransactionIntent> detectTransactionIntent(String userId, String message) {
        String systemInstruction = 
            "You are a financial assistant API for a money transfer application. " +
            "Classify the user's intent into one of these categories: " +
            "TRANSFER (sending money now), SCHEDULED_TRANSFER (sending money in the future), " +
            "WITHDRAWAL (taking money out to a bank account), CHECK_BALANCE (asking about current balance), " +
            "LIST_BENEFICIARIES (asking about who they can send to), TRANSACTION_HISTORY (asking about past transactions), " +
            "HELP (general questions or assistance related to the app), " +
            "OUT_OF_SCOPE (unrelated to financial transactions or the app), " +
            "UNKNOWN (can't determine with confidence). " +
            "Only respond with the category name in uppercase, nothing else. " +
            "If the message is definitely not related to the application or financial matters, " +
            "classify it as OUT_OF_SCOPE rather than UNKNOWN.";
        
        return geminiAiClient.generateResponse(message, systemInstruction)
            .map(response -> {
                try {
                    String intent = response.trim().toUpperCase();
                    
                    // Handle the special OUT_OF_SCOPE case
                    if ("OUT_OF_SCOPE".equals(intent)) {
                        log.info("Detected out-of-scope message from user {}: '{}'", userId, message);
                        return TransactionIntent.OUT_OF_SCOPE;
                    }
                    
                    return TransactionIntent.valueOf(intent);
                } catch (Exception e) {
                    log.warn("Failed to parse intent from AI response: {}", response);
                    return TransactionIntent.UNKNOWN;
                }
            })
            .onErrorResume(e -> {
                log.error("Error detecting intent: {}", e.getMessage());
                return Mono.just(TransactionIntent.UNKNOWN);
            });
    }
}