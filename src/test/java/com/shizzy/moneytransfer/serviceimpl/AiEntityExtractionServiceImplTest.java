package com.shizzy.moneytransfer.serviceimpl;


import com.shizzy.moneytransfer.client.GeminiAiClient;
import com.shizzy.moneytransfer.dto.TransferDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;





class AiEntityExtractionServiceImplTest {

    @Mock
    private GeminiAiClient geminiAiClient;

    @InjectMocks
    private AiEntityExtractionServiceImpl service;

    private final String userId = "user123";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void extractTransferDetails_SuccessfulExtraction() {
        // Arrange
        String message = "Send $100 to John Doe for lunch";
        Map<String, Object> aiResponse = new HashMap<>();
        aiResponse.put("amount", 100);
        aiResponse.put("recipientName", "John Doe");
        aiResponse.put("note", "for lunch");

        when(geminiAiClient.generateStructuredResponse(any(), any(), eq(Map.class)))
                .thenReturn(Mono.just(aiResponse));

        // Act & Assert
        StepVerifier.create(service.extractTransferDetails(userId, message))
                .expectNextMatches(details -> 
                    details.getAmount().compareTo(new BigDecimal("100")) == 0 &&
                    "John Doe".equals(details.getRecipientName()) &&
                    "for lunch".equals(details.getNote())
                )
                .verifyComplete();
    }

    @Test
    void extractTransferDetails_WithoutNote() {
        // Arrange
        String message = "Send $100 to John Doe";
        Map<String, Object> aiResponse = new HashMap<>();
        aiResponse.put("amount", 100);
        aiResponse.put("recipientName", "John Doe");
        aiResponse.put("note", null);

        when(geminiAiClient.generateStructuredResponse(any(), any(), eq(Map.class)))
                .thenReturn(Mono.just(aiResponse));

        // Act & Assert
        StepVerifier.create(service.extractTransferDetails(userId, message))
                .expectNextMatches(details -> 
                    details.getAmount().compareTo(new BigDecimal("100")) == 0 &&
                    "John Doe".equals(details.getRecipientName()) &&
                    details.getNote() == null
                )
                .verifyComplete();
    }

    @Test
    void extractTransferDetails_WithFormattedAmount() {
        // Arrange
        String message = "Send $1,234.56 to John Doe";
        Map<String, Object> aiResponse = new HashMap<>();
        aiResponse.put("amount", "$1,234.56");
        aiResponse.put("recipientName", "John Doe");

        when(geminiAiClient.generateStructuredResponse(any(), any(), eq(Map.class)))
                .thenReturn(Mono.just(aiResponse));

        // Act & Assert
        StepVerifier.create(service.extractTransferDetails(userId, message))
                .expectNextMatches(details -> 
                    details.getAmount().compareTo(new BigDecimal("1234.56")) == 0 &&
                    "John Doe".equals(details.getRecipientName())
                )
                .verifyComplete();
    }

    @Test
    void extractTransferDetails_AiClientError_FallbackToDefault() {
        // Arrange
        String message = "Send $100 to John";
        
        when(geminiAiClient.generateStructuredResponse(any(), any(), eq(Map.class)))
                .thenReturn(Mono.error(new RuntimeException("AI service unavailable")));

        // Act & Assert
        StepVerifier.create(service.extractTransferDetails(userId, message))
                .expectNextMatches(details -> 
                    details.getAmount().compareTo(new BigDecimal("100")) == 0 &&
                    "John".equals(details.getRecipientName())
                )
                .verifyComplete();
    }

    @Test
    void extractTransferDetails_IncompleteFallback() {
        // Arrange
        String message = "Send money please";
        
        when(geminiAiClient.generateStructuredResponse(any(), any(), eq(Map.class)))
                .thenReturn(Mono.error(new RuntimeException("AI service unavailable")));

        // Act & Assert
        StepVerifier.create(service.extractTransferDetails(userId, message))
                .expectNextMatches(details -> 
                    details.getAmount().compareTo(new BigDecimal("0.00")) == 0 &&
                    "Unknown Recipient".equals(details.getRecipientName())
                )
                .verifyComplete();
    }
}