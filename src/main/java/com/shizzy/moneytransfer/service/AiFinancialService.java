package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import reactor.core.publisher.Mono;

/**
 * Interface for AI-powered financial services that process user requests
 * using natural language understanding.
 */
public interface AiFinancialService {

    /**
     * Process a user message and determine the appropriate financial action.
     * 
     * @param userId The ID of the user sending the message
     * @param message The message from the user
     * @return A response to the user based on the processed request
     */
    Mono<ApiResponse<String>> processUserMessage(String userId, String message);
    
    /**
     * Get a conversation ID for a user to maintain context between requests.
     * 
     * @param userId The ID of the user
     * @return A unique conversation ID
     */
    String getConversationId(String userId);
    
    /**
     * Clear the conversation history for a user.
     * 
     * @param userId The ID of the user
     */
    void clearConversation(String userId);
}