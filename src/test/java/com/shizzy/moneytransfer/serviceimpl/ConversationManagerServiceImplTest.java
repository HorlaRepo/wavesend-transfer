package com.shizzy.moneytransfer.serviceimpl;


import com.shizzy.moneytransfer.client.GeminiAiClient;
import com.shizzy.moneytransfer.dto.ConversationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;





class ConversationManagerServiceImplTest {

    @Mock
    private GeminiAiClient geminiAiClient;

    private ConversationManagerServiceImpl conversationManagerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        conversationManagerService = new ConversationManagerServiceImpl(geminiAiClient);
    }

    @Test
    void generateContextualResponse_shouldReturnAiResponse() {
        // Arrange
        String userId = "user123";
        String userMessage = "Hello, I want to send money";
        String aiResponse = "Hi! I can help you send money. Who would you like to send it to?";
        
        when(geminiAiClient.continueConversation(anyList(), eq(userMessage), anyString()))
            .thenReturn(Mono.just(aiResponse));
        
        // Act
        Mono<String> result = conversationManagerService.generateContextualResponse(userId, userMessage);
        
        // Assert
        StepVerifier.create(result)
            .expectNext(aiResponse)
            .verifyComplete();
        
        verify(geminiAiClient).continueConversation(anyList(), eq(userMessage), anyString());
    }
    
    @Test
    void generateContextualResponse_shouldAddUserMessageToConversation() {
        // Arrange
        String userId = "user123";
        String userMessage = "Hello, I want to send money";
        String aiResponse = "Hi! I can help you send money.";
        
        when(geminiAiClient.continueConversation(anyList(), anyString(), anyString()))
            .thenReturn(Mono.just(aiResponse));
        
        // Act
        conversationManagerService.generateContextualResponse(userId, userMessage).block();
        
        // Assert
        ConversationState state = conversationManagerService.getConversationState(userId);
        assertEquals(userId, state.getUserId());
        
        // Create a spy to verify addMessage was called
        ConversationManagerServiceImpl spyService = spy(conversationManagerService);
        
        spyService.generateContextualResponse(userId, userMessage).block();
        verify(spyService).addMessage(eq(userId), eq("user"), eq(userMessage));
    }
    
    @Test
    void generateContextualResponse_shouldAddModelResponseToConversation() {
        // Arrange
        String userId = "user123";
        String userMessage = "Hello, I want to send money";
        String aiResponse = "Hi! I can help you send money.";
        
        when(geminiAiClient.continueConversation(anyList(), anyString(), anyString()))
            .thenReturn(Mono.just(aiResponse));
        
        // Act & Assert
        StepVerifier.create(conversationManagerService.generateContextualResponse(userId, userMessage))
            .expectNext(aiResponse)
            .verifyComplete();
        
        // Create a spy to verify addMessage was called for the model response
        ConversationManagerServiceImpl spyService = spy(conversationManagerService);
        when(geminiAiClient.continueConversation(anyList(), anyString(), anyString()))
            .thenReturn(Mono.just(aiResponse));
            
        spyService.generateContextualResponse(userId, userMessage).block();
        verify(spyService).addMessage(eq(userId), eq("model"), eq(aiResponse));
    }
    
    @Test
    void generateContextualResponse_shouldHandleErrorFromAiClient() {
        // Arrange
        String userId = "user123";
        String userMessage = "Hello, I want to send money";
        RuntimeException exception = new RuntimeException("AI service unavailable");
        
        when(geminiAiClient.continueConversation(anyList(), anyString(), anyString()))
            .thenReturn(Mono.error(exception));
        
        // Act & Assert
        StepVerifier.create(conversationManagerService.generateContextualResponse(userId, userMessage))
            .expectErrorMatches(ex -> ex instanceof RuntimeException && 
                              "AI service unavailable".equals(ex.getMessage()))
            .verify();
    }
    
    @Test
    void generateContextualResponse_shouldCreateNewConversationForNewUser() {
        // Arrange
        String userId = "newUser456";
        String userMessage = "Hello, I want to send money";
        String aiResponse = "Hi! I can help you send money.";
        
        when(geminiAiClient.continueConversation(anyList(), anyString(), anyString()))
            .thenReturn(Mono.just(aiResponse));
        
        // Act
        conversationManagerService.generateContextualResponse(userId, userMessage).block();
        
        // Assert
        ConversationState state = conversationManagerService.getConversationState(userId);
        assertNotNull(state);
        assertEquals(userId, state.getUserId());
    }
}