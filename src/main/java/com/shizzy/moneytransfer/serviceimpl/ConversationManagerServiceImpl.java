package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.client.ConversationMessage;
import com.shizzy.moneytransfer.client.GeminiAiClient;
import com.shizzy.moneytransfer.dto.ConversationState;
import com.shizzy.moneytransfer.service.ConversationManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationManagerServiceImpl implements ConversationManagerService {

    private final GeminiAiClient geminiAiClient;
    private final Map<String, ConversationState> conversations = new ConcurrentHashMap<>();
    
    @Override
    public ConversationState getConversationState(String userId) {
        return conversations.computeIfAbsent(userId, id -> {
            ConversationState state = new ConversationState();
            state.setUserId(id);
            return state;
        });
    }
    
    @Override
    public void updateConversationState(String userId, ConversationState state) {
        conversations.put(userId, state);
    }
    
    @Override
    public void addMessage(String userId, String role, String content) {
        ConversationState state = getConversationState(userId);
        state.addMessage(role, content);
    }
    
    @Override
    public Mono<String> generateContextualResponse(String userId, String message) {
        ConversationState state = getConversationState(userId);
        
        // Generate system prompt based on current state
        String systemPrompt = generateSystemPrompt(state);
        
        // Add the user message to conversation
        addMessage(userId, "user", message);
        
        // Call the AI client with the conversation history
        return geminiAiClient.continueConversation(
            state.getMessages(),
            message,
            systemPrompt
        ).doOnNext(response -> addMessage(userId, "model", response));
    }
    
    private String generateSystemPrompt(ConversationState state) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a helpful financial assistant for the WaveSend money transfer app. ");
        
        // Add context based on the current transaction stage
        switch (state.getStage()) {
            case NONE:
                prompt.append("Help the user with their financial needs. If they want to send money, " +
                             "ask for the recipient's name and amount if not provided.");
                break;
                
            case SELECTING_BENEFICIARY:
                prompt.append("The user needs to select a beneficiary from multiple matches. " +
                             "Guide them to choose by number or full name.");
                break;
                
            case CONFIRMING_TRANSACTION:
                prompt.append("The user is confirming a transaction. Please confirm details " +
                             "are correct and proceed if they approve.");
                break;
                
            case ENTERING_OTP:
                prompt.append("The user needs to enter the OTP code sent to their email. " +
                             "Remind them to check their email and enter only the code.");
                break;
                
            case TRANSACTION_COMPLETED:
                prompt.append("The transaction was completed successfully. Thank the user " +
                             "and offer assistance with anything else.");
                break;
                
            case TRANSACTION_FAILED:
                prompt.append("The transaction failed. Explain the issue and suggest next steps.");
                break;
        }
        
        prompt.append(" Be concise, friendly, and helpful.");
        return prompt.toString();
    }
}