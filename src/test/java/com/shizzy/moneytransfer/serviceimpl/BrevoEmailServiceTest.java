package com.shizzy.moneytransfer.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shizzy.moneytransfer.dto.ScheduledTransferEmailDto;
import com.shizzy.moneytransfer.dto.TransferEmailDto;
import com.shizzy.moneytransfer.enums.EmailTemplateName;
import com.shizzy.moneytransfer.enums.TransactionSource;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.service.KeycloakService;

import jakarta.mail.MessagingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.math.BigDecimal;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BrevoEmailServiceTest {

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ClassPathResource classPathResource;

    @InjectMocks
    private BrevoEmailService brevoEmailService;

    @Captor
    private ArgumentCaptor<HttpEntity<Map<String, Object>>> httpEntityCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(brevoEmailService, "apiKey", "test-api-key");
    }

    @Test
    void sendEmail_SuccessfulResponse_LogsSuccessMessage() throws Exception {
        // Arrange
        String to = "test@example.com";
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "Test User");
        EmailTemplateName templateName = EmailTemplateName.DEPOSIT;
        String subject = "Test Subject";

        // Mock template reading
        mockTemplateReading("Hello {{name}}!");

        // Mock successful API response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("messageId", "test-message-id");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://api.sendinblue.com/v3/smtp/email"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class))).thenReturn(responseEntity);

        // Act
        brevoEmailService.sendEmail(to, properties, templateName, subject);

        // Assert
        verify(restTemplate).exchange(
                eq("https://api.sendinblue.com/v3/smtp/email"),
                eq(HttpMethod.POST),
                httpEntityCaptor.capture(),
                eq(Map.class));

        HttpEntity<Map<String, Object>> capturedEntity = httpEntityCaptor.getValue();
        Map<String, Object> requestBody = capturedEntity.getBody();

        // Verify email properties
        assertEquals("WaveSend", ((Map) requestBody.get("sender")).get("name"));
        assertEquals("no-reply@wavesend.cc", ((Map) requestBody.get("sender")).get("email"));
        assertEquals(to, ((Map) ((Object[]) requestBody.get("to"))[0]).get("email"));
        assertEquals(subject, requestBody.get("subject"));
    }

    @Test
    void sendEmail_ErrorResponse_ThrowsMessagingException() throws Exception {
        // Arrange
        String to = "test@example.com";
        Map<String, Object> properties = new HashMap<>();
        EmailTemplateName templateName = EmailTemplateName.DEPOSIT;
        String subject = "Test Subject";

        // Mock template reading
        mockTemplateReading("Hello {{name}}!");

        // Mock error response
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        when(restTemplate.exchange(
                eq("https://api.sendinblue.com/v3/smtp/email"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class))).thenReturn(responseEntity);

        // Act & Assert
        assertThrows(MessagingException.class,
                () -> brevoEmailService.sendEmail(to, properties, templateName, subject));
    }

    @Test
    void sendEmail_RestClientException_ThrowsMessagingException() throws Exception {
        // Arrange
        String to = "test@example.com";
        Map<String, Object> properties = new HashMap<>();
        EmailTemplateName templateName = EmailTemplateName.DEPOSIT;
        String subject = "Test Subject";

        // Mock template reading
        mockTemplateReading("Hello {{name}}!");

        // Mock RestClientException
        when(restTemplate.exchange(
                eq("https://api.sendinblue.com/v3/smtp/email"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class))).thenThrow(new RestClientException("API connection error"));

        // Act & Assert
        assertThrows(MessagingException.class,
                () -> brevoEmailService.sendEmail(to, properties, templateName, subject));
    }

    @Test
    void defaultSendEmail_ThrowsUnsupportedOperationException() {
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> brevoEmailService.sendEmail());
    }

    @Test
    void sendCreditTransactionEmail_CallsSendEmailWithCorrectParameters() throws Exception {
        // Arrange
        Transaction transaction = createMockTransaction();
        String userEmail = "user@example.com";
        String name = "Test User";
        String subject = "Credit Transaction";

        // Mock template reading
        mockTemplateReading("Transaction template");

        // Mock successful API response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("messageId", "test-message-id");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(Map.class))).thenReturn(responseEntity);

        // Act
        brevoEmailService.sendCreditTransactionEmail(transaction, userEmail, name, subject);

        // Assert
        verify(restTemplate).exchange(
                eq("https://api.sendinblue.com/v3/smtp/email"),
                eq(HttpMethod.POST),
                httpEntityCaptor.capture(),
                eq(Map.class));

        HttpEntity<Map<String, Object>> capturedEntity = httpEntityCaptor.getValue();
        Map<String, Object> requestBody = capturedEntity.getBody();

        // Verify recipient email
        Map<String, String> toAddress = (Map<String, String>) ((Object[]) requestBody.get("to"))[0];
        assertEquals(userEmail, toAddress.get("email"));

        // Verify subject
        assertEquals(subject, requestBody.get("subject"));
    }

    @Test
    void sendDebitTransactionEmail_CallsSendEmailWithCorrectParameters() throws Exception {
        // Arrange
        Transaction transaction = createMockTransaction();
        String userEmail = "user@example.com";
        String name = "Test User";
        String subject = "Debit Transaction";

        // Mock template reading
        mockTemplateReading("Transaction template");

        // Mock successful API response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("messageId", "test-message-id");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(Map.class))).thenReturn(responseEntity);

        // Act
        brevoEmailService.sendDebitTransactionEmail(transaction, userEmail, name, subject);

        // Assert
        verify(restTemplate).exchange(
                eq("https://api.sendinblue.com/v3/smtp/email"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class));
    }

    @Test
    void sendCreditTransferEmail_CallsSendEmailWithCorrectParameters() throws Exception {
        // Arrange
        TransferEmailDto emailDto = createMockTransferEmailDto(true);

        // Mock template reading - keep this
        mockTemplateReading("Transfer template");

        // Mock successful API response - keep this
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("messageId", "test-message-id");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(Map.class))).thenReturn(responseEntity);

        // Act
        brevoEmailService.sendCreditTransferEmail(emailDto);

        // Assert - improve verification to check more properties
        verify(restTemplate).exchange(
                eq("https://api.sendinblue.com/v3/smtp/email"),
                eq(HttpMethod.POST),
                httpEntityCaptor.capture(),
                eq(Map.class));

        // Add more detailed assertions here to verify email content
        HttpEntity<Map<String, Object>> capturedRequest = httpEntityCaptor.getValue();
        Map<String, Object> requestBody = capturedRequest.getBody();

        // Verify recipient matches expected value
        Object[] toArray = (Object[]) requestBody.get("to");
        Map<String, String> toAddress = (Map<String, String>) toArray[0];
        assertEquals("receiver@example.com", toAddress.get("email"));
    }

    @Test
    void sendScheduledTransferEmail_CallsSendEmailWithCorrectParameters() throws Exception {
        // Arrange
        ScheduledTransferEmailDto emailDto = createMockScheduledTransferEmailDto();

        // Mock template reading
        mockTemplateReading("Scheduled transfer template");

        // Mock successful API response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("messageId", "test-message-id");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(HttpEntity.class),
                eq(Map.class))).thenReturn(responseEntity);

        // Act
        brevoEmailService.sendScheduledTransferEmail(emailDto);

        // Assert
        verify(restTemplate).exchange(
                eq("https://api.sendinblue.com/v3/smtp/email"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class));
    }

    // Helper methods for tests

    private void mockTemplateReading(String templateContent) throws IOException {
        // Use mockStatic to mock the static method
        try (MockedStatic<StreamUtils> streamUtilsMock = mockStatic(StreamUtils.class)) {
            streamUtilsMock.when(() -> StreamUtils.copyToString(any(InputStream.class), eq(StandardCharsets.UTF_8)))
                    .thenReturn(templateContent);
        }
    }

    private Transaction createMockTransaction() {
        Transaction transaction = mock(Transaction.class);
        Wallet wallet = mock(Wallet.class);

        // Make all stubs lenient
        lenient().when(transaction.getWallet()).thenReturn(wallet);
        lenient().when(wallet.getBalance()).thenReturn(new BigDecimal("1000.0"));
        lenient().when(transaction.getAmount()).thenReturn(new BigDecimal("100.0"));
        // Remove the duplicate call
        lenient().when(transaction.getReferenceNumber()).thenReturn("REF123456");
        lenient().when(transaction.getDescription()).thenReturn("Test transaction");
        lenient().when(transaction.getCurrentStatus()).thenReturn("COMPLETED");
        lenient().when(transaction.getSource()).thenReturn(TransactionSource.STRIPE_DEPOSIT);

        return transaction;
    }

    private TransferEmailDto createMockTransferEmailDto(boolean isCredit) {
        TransferEmailDto dto = mock(TransferEmailDto.class);
        Transaction transaction = createMockTransaction();

        when(dto.getTransaction()).thenReturn(transaction);
        when(dto.getSenderName()).thenReturn("Sender Name");
        when(dto.getReceiverName()).thenReturn("Receiver Name");
        when(dto.getSenderEmail()).thenReturn("sender@example.com");
        when(dto.getReceiverEmail()).thenReturn("receiver@example.com");
        when(dto.getSubject()).thenReturn("Transfer Notification");
        when(dto.getRecipientEmail(isCredit)).thenReturn(
                isCredit ? "receiver@example.com" : "sender@example.com");

        return dto;
    }

    private ScheduledTransferEmailDto createMockScheduledTransferEmailDto() {
        ScheduledTransferEmailDto dto = mock(ScheduledTransferEmailDto.class);

        when(dto.getSenderName()).thenReturn("Sender Name");
        when(dto.getSenderEmail()).thenReturn("sender@example.com");
        when(dto.getReceiverEmail()).thenReturn("receiver@example.com");
        when(dto.getAmount()).thenReturn(new BigDecimal("100.0"));
        when(dto.getAmount()).thenReturn(new BigDecimal("100.0"));
        when(dto.getTransferId()).thenReturn(123L);
        when(dto.getScheduledDateTime()).thenReturn(LocalDateTime.now().plusDays(1));
        when(dto.getDescription()).thenReturn("Scheduled transfer test");
        when(dto.isRecurring()).thenReturn(false);
        when(dto.getSubject()).thenReturn("Scheduled Transfer Notification");

        return dto;
    }
}