package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.dto.ConversationState;
import reactor.core.publisher.Mono;

/**
 * Service to manage conversation state and flow for multi-turn interactions
 */
public interface ConversationManagerService {
    
    /**
     * Get the current conversation state for a user
     * 
     * @param userId The ID of the user
     * @return The current conversation state
     */
    ConversationState getConversationState(String userId);
    
    /**
     * Update the conversation state for a user
     * 
     * @param userId The ID of the user
     * @param state The new conversation state
     */
    void updateConversationState(String userId, ConversationState state);
    
    /**
     * Add a message to the conversation history
     * 
     * @param userId The ID of the user
     * @param role The role (user or model)
     * @param content The message content
     */
    void addMessage(String userId, String role, String content);
    
    /**
     * Generate a contextual response based on conversation history
     * 
     * @param userId The ID of the user
     * @param message The new user message
     * @return The AI response
     */
    Mono<String> generateContextualResponse(String userId, String message);
}