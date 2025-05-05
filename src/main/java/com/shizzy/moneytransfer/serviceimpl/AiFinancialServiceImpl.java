package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.ConversationState;
import com.shizzy.moneytransfer.service.AiFinancialService;
import com.shizzy.moneytransfer.service.AiIntentDetectionService;
import com.shizzy.moneytransfer.service.ConversationManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFinancialServiceImpl implements AiFinancialService {

    private final AiIntentDetectionService intentDetectionService;
    private final ConversationManagerService conversationManager;
    private final AiScheduledTransferServiceImpl scheduledTransferService;
    private final AiInstantTransferServiceImpl instantTransferService;
    private final Random random = new Random();

    
    @Override
    public Mono<ApiResponse<String>> processUserMessage(String userId, String message) {
        log.info("Processing message from user {}: {}", userId, message);
        
        // Get conversation state
        ConversationState state = conversationManager.getConversationState(userId);
        
        // If we're in the middle of a flow, continue with that flow
        if (state.getStage() != ConversationState.TransactionStage.NONE) {
            return handleOngoingFlow(userId, message, state)
                .map(this::createSuccessResponse);
        }
        
        // Detect intent from user message
        return intentDetectionService.detectTransactionIntent(userId, message)
            .flatMap(intent -> {
                log.info("Detected intent: {}", intent);
                state.setIntent(intent);
                
                // Based on detected intent, delegate to appropriate service
                return switch (intent) {
                    case SCHEDULED_TRANSFER -> scheduledTransferService.handleScheduledTransferRequest(userId, message, state);
                    case TRANSFER -> instantTransferService.handleInstantTransferRequest(userId, message, state);
                    //case WITHDRAWAL -> withdrawalService.handleWithdrawalRequest(userId, message, state);
                    case OUT_OF_SCOPE -> Mono.just(generateOutOfScopeResponse());
                    case UNKNOWN -> Mono.just("I'm not sure what you're asking. I can help with transfers, scheduled payments, or checking your account information. How can I assist you today?");
                    default -> conversationManager.generateContextualResponse(userId, message);
                };
            })
            .map(this::createSuccessResponse);
    }
    
    private Mono<String> handleOngoingFlow(String userId, String message, ConversationState state) {
        // Delegate to the appropriate service based on the current intent
        return switch (state.getIntent()) {
            case SCHEDULED_TRANSFER -> scheduledTransferService.handleScheduledTransferRequest(userId, message, state);
            case TRANSFER -> instantTransferService.handleInstantTransferRequest(userId, message, state);
            //case WITHDRAWAL -> withdrawalService.handleWithdrawalRequest(userId, message, state);
            case OUT_OF_SCOPE -> Mono.just(generateOutOfScopeResponse());
            default -> conversationManager.generateContextualResponse(userId, message);
        };
    }

    /**
     * Provides a friendly response to guide users back to financial topics
     * when they've asked about something outside the scope of this assistant.
     */
    private String generateOutOfScopeResponse() {
        // Array of helpful responses that guide the user back to financial topics
        String[] responses = {
            "I'm your financial assistant focused on helping with money transfers and account services. " +
            "I can help you send money, schedule payments, or check your account. What financial task can I help with today?",
            
            "It seems your question is outside my capabilities as a financial assistant. " +
            "I'm here to help with transfers, withdrawals, and managing your account. " +
            "Would you like to make a transfer, schedule a payment, or check your balance?",
            
            "As your financial assistant, I'm specialized in helping with transactions and account services. " +
            "For other topics, you might want to contact customer support. " +
            "How can I assist with your financial needs today?",
            
            "I'm designed to help with financial transactions like sending money or checking your balance. " +
            "Let's focus on your financial needs - would you like to make a transfer or review your account?"
        };
        
        // Select a random response to keep interactions fresh
        return responses[random.nextInt(responses.length)];
    }
    
    private ApiResponse<String> createSuccessResponse(String message) {
        return ApiResponse.<String>builder()
            .success(true)
            .message("AI response generated")
            .data(message)
            .build();
    }
    
    @Override
    public String getConversationId(String userId) {
        // Generate a unique ID for this conversation
        // In a real implementation, this could be stored in a database
        return userId + "-" + UUID.randomUUID().toString();
    }
    
    @Override
    public void clearConversation(String userId) {
        ConversationState state = conversationManager.getConversationState(userId);
        state.reset();
        state.getMessages().clear();
    }
}