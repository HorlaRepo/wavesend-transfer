package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.client.GeminiAiClient;
import com.shizzy.moneytransfer.dto.TransactionIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;




@ExtendWith(MockitoExtension.class)
class AiIntentDetectionServiceImplTest {

    @Mock
    private GeminiAiClient geminiAiClient;

    @InjectMocks
    private AiIntentDetectionServiceImpl aiIntentDetectionService;

    private static final String USER_ID = "test-user-id";
    private static final String TEST_MESSAGE = "Test message";

    @BeforeEach
    void setUp() {
        when(geminiAiClient.generateResponse(anyString(), anyString()))
                .thenReturn(Mono.just("UNKNOWN"));
    }

    @Test
    void detectTransactionIntent_shouldReturnTransfer_whenAiReturnsTransfer() {
        when(geminiAiClient.generateResponse(anyString(), anyString()))
                .thenReturn(Mono.just("TRANSFER"));

        StepVerifier.create(aiIntentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .expectNext(TransactionIntent.TRANSFER)
                .verifyComplete();
    }

    @Test
    void detectTransactionIntent_shouldReturnScheduledTransfer_whenAiReturnsScheduledTransfer() {
        when(geminiAiClient.generateResponse(anyString(), anyString()))
                .thenReturn(Mono.just("SCHEDULED_TRANSFER"));

        StepVerifier.create(aiIntentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .expectNext(TransactionIntent.SCHEDULED_TRANSFER)
                .verifyComplete();
    }

    @Test
    void detectTransactionIntent_shouldReturnCheckBalance_whenAiReturnsCheckBalance() {
        when(geminiAiClient.generateResponse(anyString(), anyString()))
                .thenReturn(Mono.just("CHECK_BALANCE"));

        StepVerifier.create(aiIntentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .expectNext(TransactionIntent.CHECK_BALANCE)
                .verifyComplete();
    }

    @Test
    void detectTransactionIntent_shouldReturnOutOfScope_whenAiReturnsOutOfScope() {
        when(geminiAiClient.generateResponse(anyString(), anyString()))
                .thenReturn(Mono.just("OUT_OF_SCOPE"));

        StepVerifier.create(aiIntentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .expectNext(TransactionIntent.OUT_OF_SCOPE)
                .verifyComplete();
    }

    @Test
    void detectTransactionIntent_shouldHandleWhitespace_inAiResponse() {
        when(geminiAiClient.generateResponse(anyString(), anyString()))
                .thenReturn(Mono.just("  TRANSFER  \n"));

        StepVerifier.create(aiIntentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .expectNext(TransactionIntent.TRANSFER)
                .verifyComplete();
    }

    @Test
    void detectTransactionIntent_shouldReturnUnknown_whenAiReturnsInvalidIntent() {
        when(geminiAiClient.generateResponse(anyString(), anyString()))
                .thenReturn(Mono.just("INVALID_INTENT"));

        StepVerifier.create(aiIntentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .expectNext(TransactionIntent.UNKNOWN)
                .verifyComplete();
    }

    @Test
    void detectTransactionIntent_shouldReturnUnknown_whenAiReturnsEmptyString() {
        when(geminiAiClient.generateResponse(anyString(), anyString()))
                .thenReturn(Mono.just(""));

        StepVerifier.create(aiIntentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .expectNext(TransactionIntent.UNKNOWN)
                .verifyComplete();
    }

    @Test
    void detectTransactionIntent_shouldReturnUnknown_whenAiClientThrowsError() {
        when(geminiAiClient.generateResponse(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("AI service error")));

        StepVerifier.create(aiIntentDetectionService.detectTransactionIntent(USER_ID, TEST_MESSAGE))
                .expectNext(TransactionIntent.UNKNOWN)
                .verifyComplete();
    }

    @Test
    void detectTransactionIntent_shouldPassCorrectParametersToAiClient() {
        String userMessage = "How do I send money?";
        when(geminiAiClient.generateResponse(anyString(), anyString()))
                .thenReturn(Mono.just("TRANSFER"));

        aiIntentDetectionService.detectTransactionIntent(USER_ID, userMessage).block();

        verify(geminiAiClient).generateResponse(eq(userMessage), anyString());
    }
}