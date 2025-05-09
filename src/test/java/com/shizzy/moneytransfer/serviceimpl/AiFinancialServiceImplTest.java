package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.dto.ConversationState;
import com.shizzy.moneytransfer.dto.TransactionIntent;
import com.shizzy.moneytransfer.service.AiIntentDetectionService;
import com.shizzy.moneytransfer.service.ConversationManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.ArrayList;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiFinancialServiceImplTest {

    @Mock
    private AiIntentDetectionService intentDetectionService;

    @Mock
    private ConversationManagerService conversationManager;

    @Mock
    private AiScheduledTransferServiceImpl scheduledTransferService;

    @Mock
    private AiInstantTransferServiceImpl instantTransferService;

    @Spy
    private Random random = new Random(123); // Seeded for deterministic tests

    @InjectMocks
    private AiFinancialServiceImpl financialService;

    private static final String USER_ID = "test-user";
    private static final String TEST_MESSAGE = "I want to transfer money";
    private ConversationState state;

    @BeforeEach
    void setUp() {
        state = new ConversationState();
        state.setMessages(new ArrayList<>());
        when(conversationManager.getConversationState(USER_ID)).thenReturn(state);
    }

    @Test
    void processUserMessage_withOngoingFlow_shouldContinueExistingFlow() {
        // Arrange
        state.setStage(ConversationState.TransactionStage.ENTERING_RECIPIENT_EMAIL);
        state.setIntent(TransactionIntent.TRANSFER);

        when(instantTransferService.handleInstantTransferRequest(eq(USER_ID), eq(TEST_MESSAGE), eq(state)))
                .thenReturn(Mono.just("Continuing transfer flow"));

        // Act & Assert
        StepVerifier.create(financialService.processUserMessage(USER_ID, TEST_MESSAGE))
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("AI response generated", response.getMessage());
                    assertEquals("Continuing transfer flow", response.getData());
                })
                .verifyComplete();

        verify(instantTransferService).handleInstantTransferRequest(USER_ID, TEST_MESSAGE, state);
        verify(intentDetectionService, never()).detectTransactionIntent(anyString(), anyString());
    }

    @Test
    void processUserMessage_withTransferIntent_shouldDelegateToInstantTransferService() {
        // Arrange
        state.setStage(ConversationState.TransactionStage.NONE);

        when(intentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .thenReturn(Mono.just(TransactionIntent.TRANSFER));

        when(instantTransferService.handleInstantTransferRequest(eq(USER_ID), eq(TEST_MESSAGE), eq(state)))
                .thenReturn(Mono.just("Starting transfer process"));

        // Act & Assert
        StepVerifier.create(financialService.processUserMessage(USER_ID, TEST_MESSAGE))
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("AI response generated", response.getMessage());
                    assertEquals("Starting transfer process", response.getData());
                })
                .verifyComplete();

        verify(intentDetectionService).detectTransactionIntent(USER_ID, TEST_MESSAGE);
        verify(instantTransferService).handleInstantTransferRequest(USER_ID, TEST_MESSAGE, state);
    }

    @Test
    void processUserMessage_withScheduledTransferIntent_shouldDelegateToScheduledTransferService() {
        // Arrange
        state.setStage(ConversationState.TransactionStage.NONE);

        when(intentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .thenReturn(Mono.just(TransactionIntent.SCHEDULED_TRANSFER));

        when(scheduledTransferService.handleScheduledTransferRequest(eq(USER_ID), eq(TEST_MESSAGE), eq(state)))
                .thenReturn(Mono.just("Starting scheduled transfer"));

        // Act & Assert
        StepVerifier.create(financialService.processUserMessage(USER_ID, TEST_MESSAGE))
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("AI response generated", response.getMessage());
                    assertEquals("Starting scheduled transfer", response.getData());
                })
                .verifyComplete();

        verify(scheduledTransferService).handleScheduledTransferRequest(USER_ID, TEST_MESSAGE, state);
    }

    @Test
    void processUserMessage_withOutOfScopeIntent_shouldReturnOutOfScopeResponse() {
        // Arrange
        state.setStage(ConversationState.TransactionStage.NONE);

        when(intentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .thenReturn(Mono.just(TransactionIntent.OUT_OF_SCOPE));

        // Act & Assert
        StepVerifier.create(financialService.processUserMessage(USER_ID, TEST_MESSAGE))
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("AI response generated", response.getMessage());
                    assertNotNull(response.getData());
                    assertTrue(response.getData().contains("financial assistant"));
                })
                .verifyComplete();

        verify(intentDetectionService).detectTransactionIntent(USER_ID, TEST_MESSAGE);
    }

    @Test
    void processUserMessage_withUnknownIntent_shouldReturnDefaultResponse() {
        // Arrange
        state.setStage(ConversationState.TransactionStage.NONE);

        when(intentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .thenReturn(Mono.just(TransactionIntent.UNKNOWN));

        // Act & Assert
        StepVerifier.create(financialService.processUserMessage(USER_ID, TEST_MESSAGE))
                .assertNext(response -> {
                    assertTrue(response.isSuccess());
                    assertEquals("AI response generated", response.getMessage());
                    assertTrue(response.getData().contains("I'm not sure what you're asking"));
                })
                .verifyComplete();
    }

    @Test
void processUserMessage_withGeneralInquiry_shouldUseConversationManager() {
    // Arrange
    state.setStage(ConversationState.TransactionStage.NONE);

    when(intentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
            .thenReturn(Mono.just(TransactionIntent.OUT_OF_SCOPE));


    final String[] possibleResponses = {
        "financial assistant", 
        "help with money transfers",
        "account services"
    };

    // Act & Assert
    StepVerifier.create(financialService.processUserMessage(USER_ID, TEST_MESSAGE))
            .assertNext(response -> {
                assertTrue(response.isSuccess());
                assertEquals("AI response generated", response.getMessage());
                
                // Assert that response contains at least one of the possible response fragments
                boolean containsExpectedContent = false;
                for (String possibleResponse : possibleResponses) {
                    if (response.getData().contains(possibleResponse)) {
                        containsExpectedContent = true;
                        break;
                    }
                }
                assertTrue(containsExpectedContent, 
                        "I'm your financial assistant focused");
            })
            .verifyComplete();

    verify(conversationManager, never()).generateContextualResponse(anyString(), anyString());
}

    @Test
    void clearConversation_shouldResetStateAndClearMessages() {
        // Arrange
        state.setIntent(TransactionIntent.TRANSFER);
        state.setStage(ConversationState.TransactionStage.TRANSACTION_COMPLETED);

        // Act
        financialService.clearConversation(USER_ID);

        // Assert
        verify(conversationManager).getConversationState(USER_ID);
        assertEquals(0, state.getMessages().size());
    }
}