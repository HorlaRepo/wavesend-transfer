package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.dto.TransactionIntent;
import reactor.core.publisher.Mono;

/**
 * Service to detect user intents from natural language messages
 */
public interface AiIntentDetectionService {
    
    /**
     * Detect the transaction intent from a user message
     * 
     * @param userId The ID of the user
     * @param message The message from the user
     * @return The detected transaction intent
     */
    Mono<TransactionIntent> detectTransactionIntent(String userId, String message);
}