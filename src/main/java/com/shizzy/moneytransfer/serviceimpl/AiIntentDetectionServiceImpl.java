package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.client.GeminiAiClient;
import com.shizzy.moneytransfer.dto.TransactionIntent;
import com.shizzy.moneytransfer.service.AiIntentDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiIntentDetectionServiceImpl implements AiIntentDetectionService {

    private final GeminiAiClient geminiAiClient;

    private static final List<Pattern> CHECK_BALANCE_PATTERNS = List.of(
            Pattern.compile("^balance\\??$"),
            Pattern.compile("\\bcheck (?:my )?(?:wallet |account )?balance\\b"),
            Pattern.compile("\\b(?:what(?:'s| is)|show me|tell me) (?:my )?(?:wallet |account )?balance\\b"),
            Pattern.compile("\\b(?:current|available|remaining) balance\\b"),
            Pattern.compile("\\bwallet balance\\b"),
            Pattern.compile("\\bhow much (?:money )?(?:do i have|is in my wallet|is left in my wallet)\\b"));
    
    @Override
    public Mono<TransactionIntent> detectTransactionIntent(String userId, String message) {
        if (isBalanceCheckMessage(message)) {
            log.info("Detected balance check intent from keywords for user {}: '{}'", userId, message);
            return Mono.just(TransactionIntent.CHECK_BALANCE);
        }

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

    private boolean isBalanceCheckMessage(String message) {
        if (message == null) {
            return false;
        }

        String normalizedMessage = message.trim().toLowerCase(Locale.ROOT);
        if (normalizedMessage.isBlank()) {
            return false;
        }

        return CHECK_BALANCE_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(normalizedMessage).find());
    }
}
