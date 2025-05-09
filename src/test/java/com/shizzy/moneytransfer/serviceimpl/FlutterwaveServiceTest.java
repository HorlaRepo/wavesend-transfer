package com.shizzy.moneytransfer.serviceimpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionStatus;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.kafka.NotificationProducer;
import com.shizzy.moneytransfer.model.BankAccount;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.TransactionReference;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.TransactionReferenceRepository;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.TransactionReferenceService;
import com.shizzy.moneytransfer.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FlutterwaveServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionReferenceService referenceService;

    @Mock
    private TransactionReferenceRepository referenceRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private NotificationProducer notificationProducer;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    @Spy
    private FlutterwaveService flutterwaveService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(flutterwaveService, "apiKey", "test_api_key");
        ReflectionTestUtils.setField(flutterwaveService, "baseUrl", "https://api.flutterwave.com/v3");
        ReflectionTestUtils.setField(flutterwaveService, "webClient", webClient);
        ReflectionTestUtils.setField(flutterwaveService, "notificationProducer", notificationProducer);
    }

    @Test
    void testBuildFlutterwaveWithdrawalRequest_Africa() {
        // Arrange
        BankAccount bankAccount = new BankAccount();
        bankAccount.setRegion("africa");
        bankAccount.setBankCode("044");
        bankAccount.setAccountNumber("0690000031");

        WithdrawalInfo withdrawalInfo = new WithdrawalInfo();
        withdrawalInfo.setBankAccount(bankAccount);
        withdrawalInfo.setAmount(100.0);
        withdrawalInfo.setCurrency("NGN");
        withdrawalInfo.setNarration("Test withdrawal");

        String reference = "REF123456";

        // Act
        FlutterwaveWithdrawalRequest result = flutterwaveService.buildFlutterwaveWithdrawalRequest(withdrawalInfo,
                reference);

        // Assert
        assertEquals(100.0, result.getAmount());
        assertEquals("NGN", result.getCurrency());
        assertEquals("Test withdrawal", result.getNarration());
        assertEquals("REF123456", result.getReference());
        assertEquals("044", result.getAccount_bank());
        assertEquals("0690000031", result.getAccount_number());
    }

    @Test
    void testBuildFlutterwaveWithdrawalRequest_EU() {
        // Arrange
        BankAccount bankAccount = new BankAccount();
        bankAccount.setRegion("eu");
        bankAccount.setBankName("Deutsche Bank");
        bankAccount.setAccountNumber("DE89370400440532013000");
        bankAccount.setSwiftCode("DEUTDEDBXXX");
        bankAccount.setBeneficiaryName("John Doe");
        bankAccount.setBeneficiaryCountry("DE");
        bankAccount.setPostalCode("10117");
        bankAccount.setStreetNumber("123");
        bankAccount.setStreetName("Berlin Strasse");
        bankAccount.setCity("Berlin");

        WithdrawalInfo withdrawalInfo = new WithdrawalInfo();
        withdrawalInfo.setBankAccount(bankAccount);
        withdrawalInfo.setAmount(100.0);
        withdrawalInfo.setCurrency("EUR");
        withdrawalInfo.setNarration("Test EU withdrawal");

        String reference = "EUREF123456";

        // Act
        FlutterwaveWithdrawalRequest result = flutterwaveService.buildFlutterwaveWithdrawalRequest(withdrawalInfo,
                reference);

        // Assert
        assertEquals(100.0, result.getAmount());
        assertEquals("EUR", result.getCurrency());
        assertEquals("Test EU withdrawal", result.getNarration());
        assertEquals("EUREF123456", result.getReference());
        assertEquals("John Doe", result.getBeneficiary_name());
        assertNotNull(result.getMeta());
        assertEquals(1, result.getMeta().size());
        assertEquals("Deutsche Bank", result.getMeta().get(0).getBank_name());
        assertEquals("DE89370400440532013000", result.getMeta().get(0).getAccount_number());
    }

    @Test
    void testHandleWebhook_SuccessfulTransaction() {
        // Arrange
        WithdrawalData data = new WithdrawalData();
        data.setStatus("SUCCESSFUL");
        data.setReference("REF123456");
        data.setNarration("Successful transfer");

        WebhookPayload payload = new WebhookPayload();
        payload.setEvent("transfer.completed");
        payload.setData(data);

        Transaction transaction = new Transaction();
        transaction.setCurrentStatus(TransactionStatus.PENDING.getValue());
        transaction.setReferenceNumber("REF123456");
        Wallet wallet = new Wallet();
        wallet.setCreatedBy("user123");
        transaction.setWallet(wallet);

        when(transactionRepository.findTransactionByReferenceNumber("REF123456"))
                .thenReturn(Collections.singletonList(transaction));

        // Mock the KeycloakService.getUserById method
        ApiResponse<org.keycloak.representations.idm.UserRepresentation> userResponse = new ApiResponse<>();
        userResponse.setSuccess(true);
        org.keycloak.representations.idm.UserRepresentation userRep = new org.keycloak.representations.idm.UserRepresentation();
        userRep.setId("user123");
        userRep.setFirstName("John");
        userRep.setLastName("Doe");
        userRep.setEmail("john.doe@example.com");
        userResponse.setData(userRep);

        when(keycloakService.getUserById("user123")).thenReturn(userResponse);

        doNothing().when(notificationProducer).sendNotification(eq("notifications"),
                any(TransactionNotification.class));

        // Act
        ResponseEntity<String> response = flutterwaveService.handleWebhook(payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Webhook received", response.getBody());
        verify(transactionRepository).save(transaction);
        assertEquals(TransactionStatus.SUCCESS.getValue(), transaction.getCurrentStatus());
    }

    @Test
    void testHandleWebhook_FailedTransaction() {
        // Arrange
        WithdrawalData data = new WithdrawalData();
        data.setStatus("FAILED");
        data.setReference("REF123456");
        data.setNarration("Failed transfer");

        WebhookPayload payload = new WebhookPayload();
        payload.setEvent("transfer.completed");
        payload.setData(data);

        Transaction transaction = new Transaction();
        transaction.setCurrentStatus(TransactionStatus.PENDING.getValue());
        transaction.setReferenceNumber("REF123456");
        transaction.setAmount(BigDecimal.valueOf(100.0));
        transaction.setFee(5.0);

        Wallet wallet = new Wallet();
        wallet.setCreatedBy("user123");
        transaction.setWallet(wallet);

        when(transactionRepository.findTransactionByReferenceNumber("REF123456"))
                .thenReturn(Collections.singletonList(transaction));

        // Act
        ResponseEntity<String> response = flutterwaveService.handleWebhook(payload);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Webhook received", response.getBody());
        // Change from times(2) to times(3)
        verify(transactionRepository, times(3)).save(any(Transaction.class));
        assertEquals(TransactionStatus.FAILED.getValue(), transaction.getCurrentStatus());
        assertEquals("Failed transfer /FAILED", transaction.getNarration());
    }

    @Test
    void testGetFees() {
        // Arrange
        GenericResponse<List<FeeData>> expectedResponse = new GenericResponse<>();
        expectedResponse.setStatus("success");

        FeeData feeData = new FeeData();
        feeData.setFee(5.0);
        expectedResponse.setData(Collections.singletonList(feeData));

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // Act
        GenericResponse<List<FeeData>> result = flutterwaveService.getFees(100.0, "NGN");

        // Assert
        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals(1, result.getData().size());
        assertEquals(5.0, result.getData().get(0).getFee());
    }

    @Test
    void testAddBeneficiary() {
        // Arrange
        AddBeneficiaryRequest request = new AddBeneficiaryRequest();
        request.setAccount_number("0690000031");
        request.setAccount_bank("044");

        GenericResponse<Beneficiary> expectedResponse = new GenericResponse<>();
        expectedResponse.setStatus("success");

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setId(12345);
        beneficiary.setAccount_number("0690000031");
        expectedResponse.setData(beneficiary);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // Act
        GenericResponse<Beneficiary> result = flutterwaveService.addBeneficiary(request);

        // Assert
        assertNotNull(result);
        assertEquals("success", result.getStatus());
        assertEquals(12345, result.getData().getId());
        assertEquals("0690000031", result.getData().getAccount_number());
    }
}