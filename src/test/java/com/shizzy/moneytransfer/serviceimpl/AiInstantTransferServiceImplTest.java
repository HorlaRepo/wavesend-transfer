package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.AccountLimitDTO;
import com.shizzy.moneytransfer.dto.ConversationState;
import com.shizzy.moneytransfer.dto.CreateTransactionRequestBody;
import com.shizzy.moneytransfer.dto.TransactionResponseDTO;
import com.shizzy.moneytransfer.dto.TransferDetails;
import com.shizzy.moneytransfer.dto.TransferInitiationResponse;
import com.shizzy.moneytransfer.dto.TransferVerificationRequest;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.model.UserBeneficiaries;
import com.shizzy.moneytransfer.model.UserBeneficiary;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.UserBeneficiariesRepository;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.AccountLimitService;
import com.shizzy.moneytransfer.service.AiEntityExtractionService;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.MoneyTransferService;
import com.shizzy.moneytransfer.service.TransactionLimitService;

import io.swagger.annotations.Api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiInstantTransferServiceImplTest {

    @Mock
    private UserBeneficiariesRepository beneficiaryRepository;

    @Mock
    private AiEntityExtractionService aiEntityExtractionService;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private MoneyTransferService moneyTransferService;

    @Mock
    private AccountLimitService accountLimitService;

    @Mock
    private TransactionLimitService transactionLimitService;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private AiInstantTransferServiceImpl service;

    private String userId;
    private ConversationState state;
    private TransferDetails transferDetails;
    private UserRepresentation user;
    private ApiResponse<UserRepresentation> userResponse;

    @BeforeEach
    public void setup() {
        userId = "user123";
        state = new ConversationState();

        transferDetails = new TransferDetails(
                new BigDecimal("100.00"),
                "John Doe",
                "Test transfer");

        user = new UserRepresentation();
        user.setId(userId);
        user.setEmail("sender@example.com");

        userResponse = ApiResponse.<UserRepresentation>builder()
                .success(true)
                .data(user)
                .message("Success")
                .build();
    }

    @Test
    public void testHandleInstantTransferRequest_NoneState() {
        String message = "Send $100 to John";
        state.setStage(ConversationState.TransactionStage.NONE);

        when(aiEntityExtractionService.extractTransferDetails(eq(userId), eq(message)))
                .thenReturn(Mono.just(transferDetails));
        when(accountLimitService.wouldExceedTransferLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(accountLimitService.wouldExceedDailyLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(keycloakService.getUserById(userId)).thenReturn(userResponse);

        List<UserBeneficiaries> emptyList = new ArrayList<>();
        when(beneficiaryRepository.findByUserIdWithBeneficiaries(userId)).thenReturn(emptyList);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(
                        response -> response.contains("I couldn't find any beneficiaries matching 'John Doe'"))
                .verifyComplete();

        verify(aiEntityExtractionService).extractTransferDetails(eq(userId), eq(message));
    }

    @Test
    public void testHandleInstantTransferRequest_SelectingBeneficiary() {
        String message = "1";
        state.setStage(ConversationState.TransactionStage.SELECTING_BENEFICIARY);

        UserBeneficiary beneficiary = new UserBeneficiary();
        beneficiary.setName("John Doe");
        beneficiary.setEmail("john@example.com");

        List<UserBeneficiary> beneficiaries = List.of(beneficiary);
        state.setMatchingBeneficiaries(beneficiaries);
        state.setAmount(new BigDecimal("100.00"));

        when(keycloakService.getUserById(userId)).thenReturn(userResponse);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("I'm about to transfer $100.00 to John Doe") &&
                        response.contains("sender@example.com"))
                .verifyComplete();

        verify(keycloakService).getUserById(userId);

        // Verify state changes
        assert state.getSelectedBeneficiaryEmail().equals("john@example.com");
        assert state.getStage() == ConversationState.TransactionStage.CONFIRMING_TRANSACTION;
    }

    @Test
    public void testHandleInstantTransferRequest_EnteringRecipientEmail() {
        String message = "john@example.com";
        state.setStage(ConversationState.TransactionStage.ENTERING_RECIPIENT_EMAIL);
        state.setAmount(new BigDecimal("100.00"));

        ApiResponse<UserRepresentation> recipientResponse = ApiResponse.<UserRepresentation>builder()
                .success(true)
                .data(new UserRepresentation())
                .message("Success")
                .build();

        when(keycloakService.existsUserByEmail(anyString())).thenReturn(recipientResponse);
        when(keycloakService.getUserById(userId)).thenReturn(userResponse);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("I'm about to transfer $100.00 to john@example.com") &&
                        response.contains("sender@example.com"))
                .verifyComplete();

        verify(keycloakService).getUserById(userId);

        // Verify state changes
        assert state.getSelectedBeneficiaryEmail().equals("john@example.com");
        assert state.getStage() == ConversationState.TransactionStage.CONFIRMING_TRANSACTION;
    }

    @Test
    public void testHandleInstantTransferRequest_ConfirmingTransactionYes() {
        String message = "yes";
        state.setStage(ConversationState.TransactionStage.CONFIRMING_TRANSACTION);
        state.setSelectedBeneficiaryEmail("john@example.com");
        state.setAmount(new BigDecimal("100.00"));

        when(keycloakService.getUserById(userId)).thenReturn(userResponse);

        TransferInitiationResponse initiationResponse = new TransferInitiationResponse();
        initiationResponse.setTransferToken("token123");
        ApiResponse<TransferInitiationResponse> response = ApiResponse.<TransferInitiationResponse>builder()
                .success(true)
                .data(initiationResponse)
                .message("Success")
                .build();

        when(moneyTransferService.initiateTransfer(any(CreateTransactionRequestBody.class), eq(userId)))
                .thenReturn(response);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response1 -> response1.contains("I've initiated your transfer") &&
                        response1.contains("verification code"))
                .verifyComplete();

        verify(moneyTransferService).initiateTransfer(any(CreateTransactionRequestBody.class), eq(userId));

        // Verify state changes
        assert state.getTransferToken().equals("token123");
        assert state.getStage() == ConversationState.TransactionStage.ENTERING_OTP;
    }

    @Test
    public void testHandleInstantTransferRequest_ConfirmingTransactionNo() {
        String message = "no";
        state.setStage(ConversationState.TransactionStage.CONFIRMING_TRANSACTION);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNext("I've cancelled the transfer. Is there anything else you'd like to do?")
                .verifyComplete();

        // Verify state was reset
        assert state.getStage() == ConversationState.TransactionStage.NONE;
    }

    @Test
    public void testHandleInstantTransferRequest_EnteringOTP() {
        String message = "123456";
        state.setStage(ConversationState.TransactionStage.ENTERING_OTP);
        state.setTransferToken("token123");
        state.setSelectedBeneficiaryEmail("john@example.com");

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                LocalDateTime.now().toString(),
                new BigDecimal("100.00"),
                "Completed",
                TransactionType.DEBIT,
                "Sender Name",
                "sender@example.com",
                "John Doe",
                "john@example.com",
                "123");
        ApiResponse<TransactionResponseDTO> response = ApiResponse.<TransactionResponseDTO>builder()
                .success(true)
                .data(responseDTO)
                .message("Success")
                .build();

        when(moneyTransferService.verifyAndTransfer(any(TransferVerificationRequest.class), eq(userId)))
                .thenReturn(response);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(responseText -> responseText.contains("Success!") &&
                        responseText.contains("$100.00") &&
                        responseText.contains("Reference Number: 123"))
                .verifyComplete();

        verify(moneyTransferService).verifyAndTransfer(any(TransferVerificationRequest.class), eq(userId));

        // Verify state changes
        assert state.getStage() == ConversationState.TransactionStage.TRANSACTION_COMPLETED;
    }

    @Test
    public void testHandleInstantTransferRequest_TransactionCompleted() {
        String message = "Another transfer";
        state.setStage(ConversationState.TransactionStage.TRANSACTION_COMPLETED);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNext("Type a new message to start another transaction.")
                .verifyComplete();
    }

    @Test
    public void testHandleInitialRequest_DirectEmailTransfer() {
        String message = "Send $100 to john@example.com";
        TransferDetails details = new TransferDetails(
                new BigDecimal("100.00"),
                "john@example.com",
                "Test transfer");

        when(aiEntityExtractionService.extractTransferDetails(eq(userId), eq(message)))
                .thenReturn(Mono.just(details));
        when(accountLimitService.wouldExceedTransferLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(accountLimitService.wouldExceedDailyLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(keycloakService.existsUserByEmail(anyString()))
                .thenReturn(ApiResponse.<UserRepresentation>builder()
                        .success(true)
                        .data(new UserRepresentation())
                        .message("Success")
                        .build());
        when(keycloakService.getUserById(userId)).thenReturn(userResponse);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("I'm about to transfer $100.00 to john@example.com"))
                .verifyComplete();

        verify(aiEntityExtractionService).extractTransferDetails(eq(userId), eq(message));
        verify(keycloakService).getUserById(userId);

        // Verify state changes
        assert state.getAmount().compareTo(new BigDecimal("100.00")) == 0;
        assert state.getSelectedBeneficiaryEmail().equals("john@example.com");
        assert state.getStage() == ConversationState.TransactionStage.CONFIRMING_TRANSACTION;
    }

    @Test
    public void testHandleInitialRequest_InsufficientBalance() {
        String message = "Send $1500 to John";
        TransferDetails details = new TransferDetails(
                new BigDecimal("1500.00"),
                "John",
                "Test transfer");

        when(aiEntityExtractionService.extractTransferDetails(eq(userId), eq(message)))
                .thenReturn(Mono.just(details));
        when(accountLimitService.wouldExceedTransferLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(accountLimitService.wouldExceedDailyLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(keycloakService.getUserById(userId)).thenReturn(userResponse);

        // Assuming getUserBalance returns 1000.00
        // The test will pass because the service returns fixed 1000.00

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("I'm sorry, but you don't have enough balance") &&
                        response.contains("$1000.00") &&
                        response.contains("$1500.00"))
                .verifyComplete();

        verify(aiEntityExtractionService).extractTransferDetails(eq(userId), eq(message));
        verify(keycloakService).getUserById(userId);
    }

    @Test
    public void testHandleInitialRequest_SingleBeneficiaryMatch() {
        String message = "Send $100 to John";

        when(aiEntityExtractionService.extractTransferDetails(eq(userId), eq(message)))
                .thenReturn(Mono.just(transferDetails));
        when(accountLimitService.wouldExceedTransferLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(accountLimitService.wouldExceedDailyLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(keycloakService.getUserById(userId)).thenReturn(userResponse);

        // Create a single matching beneficiary
        UserBeneficiary beneficiary = new UserBeneficiary();
        beneficiary.setName("John Doe");
        beneficiary.setEmail("john@example.com");

        UserBeneficiaries userBeneficiaries = new UserBeneficiaries();
        userBeneficiaries.setUserId(userId);
        userBeneficiaries.setBeneficiaries(List.of(beneficiary));

        when(beneficiaryRepository.findByUserIdWithBeneficiaries(userId))
                .thenReturn(List.of(userBeneficiaries));

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("I'm about to transfer $100.00 to John Doe"))
                .verifyComplete();

        verify(aiEntityExtractionService).extractTransferDetails(eq(userId), eq(message));
        verify(beneficiaryRepository).findByUserIdWithBeneficiaries(userId);

        // Verify state changes
        assert state.getSelectedBeneficiaryEmail().equals("john@example.com");
        assert state.getStage() == ConversationState.TransactionStage.CONFIRMING_TRANSACTION;
    }

    @Test
    public void testHandleInitialRequest_MultipleBeneficiaryMatches() {
        String message = "Send $100 to John";

        transferDetails = new TransferDetails(
                new BigDecimal("100.00"),
                "John", // Partial name to match multiple beneficiaries
                "Test transfer");

        when(aiEntityExtractionService.extractTransferDetails(eq(userId), eq(message)))
                .thenReturn(Mono.just(transferDetails));
        when(accountLimitService.wouldExceedTransferLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(accountLimitService.wouldExceedDailyLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(keycloakService.getUserById(userId)).thenReturn(userResponse);

        //multiple matching beneficiaries
        UserBeneficiary beneficiary1 = new UserBeneficiary();
        beneficiary1.setName("John Doe");
        beneficiary1.setEmail("john@example.com");

        UserBeneficiary beneficiary2 = new UserBeneficiary();
        beneficiary2.setName("John Smith");
        beneficiary2.setEmail("johnsmith@example.com");

        List<UserBeneficiary> matchingBeneficiaries = Arrays.asList(beneficiary1, beneficiary2);

        UserBeneficiaries userBeneficiaries = new UserBeneficiaries();
        userBeneficiaries.setUserId(userId);
        userBeneficiaries.setBeneficiaries(matchingBeneficiaries);

        when(beneficiaryRepository.findByUserIdWithBeneficiaries(userId))
                .thenReturn(List.of(userBeneficiaries));

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        // Verify the response
        StepVerifier.create(result)
                .consumeNextWith(response -> {
                    System.out.println("ACTUAL RESPONSE: " + response);
                    if (response.contains("I found multiple beneficiaries")) {
                        // If we got the multiple beneficiaries response
                        assertTrue(response.contains("1. John Doe (john@example.com)"),
                                "Response should include first beneficiary");
                        assertTrue(response.contains("2. John Smith (johnsmith@example.com)"),
                                "Response should include second beneficiary");
                    } else {
                        // If we didn't get the expected response, fail with helpful message
                        fail("Expected multiple beneficiaries prompt but got: " + response);
                    }
                })
                .verifyComplete();

        // Verify the service interactions and state
        verify(beneficiaryRepository).findByUserIdWithBeneficiaries(userId);

        assertEquals(ConversationState.TransactionStage.SELECTING_BENEFICIARY, state.getStage(),
                "State should be updated to SELECTING_BENEFICIARY");
        assertEquals(2, state.getMatchingBeneficiaries().size(),
                "State should contain 2 matching beneficiaries");
    }

    @Test
    public void testHandleOtpVerification_SuccessfulVerification() {
        String message = "123456";
        state.setStage(ConversationState.TransactionStage.ENTERING_OTP);
        state.setTransferToken("token123");
        state.setSelectedBeneficiaryEmail("john@example.com");
        state.setRecipientName("John Doe");

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                LocalDateTime.now().toString(),
                new BigDecimal("100.00"),
                "Completed",
                TransactionType.DEBIT,
                "Sender Name",
                "sender@example.com",
                "John Doe",
                "john@example.com",
                "123");
        ApiResponse<TransactionResponseDTO> response = ApiResponse.<TransactionResponseDTO>builder()
                .success(true)
                .data(responseDTO)
                .message("Success")
                .build();

        when(moneyTransferService.verifyAndTransfer(any(TransferVerificationRequest.class), eq(userId)))
                .thenReturn(response);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(responseText -> responseText.contains("Success") &&
                        responseText.contains("$100.00") &&
                        responseText.contains("John Doe") &&
                        responseText.contains("Reference Number: 123"))
                .verifyComplete();

        verify(moneyTransferService).verifyAndTransfer(any(TransferVerificationRequest.class), eq(userId));

        // Verify state changes - replaced 'assert' with proper JUnit assertions
        assertEquals(ConversationState.TransactionStage.TRANSACTION_COMPLETED, state.getStage(),
                "State should transition to TRANSACTION_COMPLETED after successful verification");
        assertEquals(0, state.getOtpAttempts(),
                "OTP attempts should be tracked correctly");
    }

    @Test
    public void testHandleOtpVerification_FailedVerification() {
        String message = "123456";
        state.setStage(ConversationState.TransactionStage.ENTERING_OTP);
        state.setTransferToken("token123");

        ApiResponse<TransactionResponseDTO> response = ApiResponse.<TransactionResponseDTO>builder()
                .success(false)
                .data(null)
                .message("Invalid OTP")
                .build();

        when(moneyTransferService.verifyAndTransfer(any(TransferVerificationRequest.class), eq(userId)))
                .thenReturn(response);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(responseText -> responseText.contains("The verification code is incorrect") &&
                        responseText.contains("You have 2 attempts remaining"))
                .verifyComplete();

        verify(moneyTransferService).verifyAndTransfer(any(TransferVerificationRequest.class), eq(userId));

        // Verify state changes
        assert state.getOtpAttempts() == 1;
        assert state.getStage() == ConversationState.TransactionStage.ENTERING_OTP;
    }

    @Test
    public void testHandleOtpVerification_TooManyFailedAttempts() {
        String message = "123456";
        state.setStage(ConversationState.TransactionStage.ENTERING_OTP);
        state.setTransferToken("token123");
        state.setOtpAttempts(2); // Already had 2 failed attempts

        ApiResponse<TransactionResponseDTO> response = ApiResponse.<TransactionResponseDTO>builder()
                .success(false)
                .data(null)
                .message("Maximum attempts reached")
                .build();

        when(moneyTransferService.verifyAndTransfer(any(TransferVerificationRequest.class), eq(userId)))
                .thenReturn(response);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(responseText -> responseText
                        .contains("you've reached the maximum number of verification attempts") &&
                        responseText.contains("cancelled"))
                .verifyComplete();

        verify(moneyTransferService).verifyAndTransfer(any(TransferVerificationRequest.class), eq(userId));

        // Verify state changes
        assert state.getOtpAttempts() == 3; // Incremented to 3
        assert state.getStage() == ConversationState.TransactionStage.TRANSACTION_FAILED;
    }

    @Test
    public void testHandleOtpResendRequest_Success() {
        String message = "resend code";
        state.setStage(ConversationState.TransactionStage.ENTERING_OTP);
        state.setSelectedBeneficiaryEmail("john@example.com");
        state.setAmount(new BigDecimal("100.00"));
        state.setOtpSentAt(LocalDateTime.now().minusMinutes(2)); // Last OTP was sent 2 minutes ago

        when(keycloakService.getUserById(userId)).thenReturn(userResponse);

        TransferInitiationResponse initiationResponse = new TransferInitiationResponse();
        initiationResponse.setTransferToken("new_token123");
        ApiResponse<TransferInitiationResponse> response = ApiResponse.<TransferInitiationResponse>builder()
                .success(true)
                .data(initiationResponse)
                .message("Success")
                .build();

        when(moneyTransferService.initiateTransfer(any(CreateTransactionRequestBody.class), eq(userId)))
                .thenReturn(response);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(responseText -> responseText.contains("I've sent a new verification code") &&
                        responseText.contains("Please enter it here"))
                .verifyComplete();

        verify(moneyTransferService).initiateTransfer(any(CreateTransactionRequestBody.class), eq(userId));

        // Verify state changes
        assert state.getTransferToken().equals("new_token123");
        assert state.getOtpAttempts() == 0; // Reset to 0
    }

    @Test
    public void testHandleOtpResendRequest_Cooldown() {
        String message = "resend code";
        state.setStage(ConversationState.TransactionStage.ENTERING_OTP);
        state.setOtpSentAt(LocalDateTime.now().minusSeconds(30)); // Last OTP was sent 30 seconds ago

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(responseText -> responseText.contains("Please wait") &&
                        responseText.contains("before requesting a new code"))
                .verifyComplete();

        // Verify no calls to initiate transfer
        verify(moneyTransferService, never()).initiateTransfer(any(), any());
    }

    @Test
    public void testHandlePreValidateTransactionLimits_ExceedsTransferLimit() {
        String message = "Send $1000 to John";

        TransferDetails details = new TransferDetails(
                new BigDecimal("1000.00"),
                "John",
                "Test transfer");

        when(aiEntityExtractionService.extractTransferDetails(eq(userId), eq(message)))
                .thenReturn(Mono.just(details));

        // Setup to exceed transfer limit
        when(accountLimitService.wouldExceedTransferLimit(eq(userId), any(BigDecimal.class)))
                .thenReturn(true);

        AccountLimitDTO limits = new AccountLimitDTO();
        limits.setMaxTransferAmount(new BigDecimal("500.00"));
        when(accountLimitService.getUserLimits(userId)).thenReturn(limits);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(responseText -> responseText.contains("exceeds your transfer limit of $500.00") &&
                        responseText.contains("Would you like to try a different amount?"))
                .verifyComplete();

        verify(aiEntityExtractionService).extractTransferDetails(eq(userId), eq(message));
        verify(accountLimitService).wouldExceedTransferLimit(eq(userId), any(BigDecimal.class));
        verify(accountLimitService).getUserLimits(userId);
    }

    @Test
    public void testHandlePreValidateTransactionLimits_ExceedsDailyLimit() {
        String message = "Send $1000 to John";

        TransferDetails details = new TransferDetails(
                new BigDecimal("1000.00"),
                "John",
                "Test transfer");

        when(aiEntityExtractionService.extractTransferDetails(eq(userId), eq(message)))
                .thenReturn(Mono.just(details));

        // Setup to not exceed transfer limit but exceed daily limit
        when(accountLimitService.wouldExceedTransferLimit(eq(userId), any(BigDecimal.class)))
                .thenReturn(false);
        when(accountLimitService.wouldExceedDailyLimit(eq(userId), any(BigDecimal.class)))
                .thenReturn(true);

        AccountLimitDTO limits = new AccountLimitDTO();
        limits.setDailyTransactionLimit(new BigDecimal("2000.00"));
        when(accountLimitService.getUserLimits(userId)).thenReturn(limits);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(
                        responseText -> responseText.contains("exceed your daily transaction limit of $2000.00") &&
                                responseText.contains("consider upgrading your verification level"))
                .verifyComplete();

        verify(aiEntityExtractionService).extractTransferDetails(eq(userId), eq(message));
        verify(accountLimitService).wouldExceedTransferLimit(eq(userId), any(BigDecimal.class));
        verify(accountLimitService).wouldExceedDailyLimit(eq(userId), any(BigDecimal.class));
        verify(accountLimitService).getUserLimits(userId);
    }

    @Test
    public void testValidateRecipientLimits_ExceedsRecipientBalanceLimit() {
        String message = "Send $100 to john@example.com";

        TransferDetails details = new TransferDetails(
                new BigDecimal("100.00"),
                "john@example.com",
                "Test transfer");

        when(aiEntityExtractionService.extractTransferDetails(eq(userId), eq(message)))
                .thenReturn(Mono.just(details));
        when(accountLimitService.wouldExceedTransferLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(accountLimitService.wouldExceedDailyLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);

        // Setup recipient validation
        UserRepresentation recipient = new UserRepresentation();
        recipient.setId("recipient123");
        recipient.setEmail("john@example.com");
        ApiResponse<UserRepresentation> recipientResponse = ApiResponse.<UserRepresentation>builder()
                .success(true)
                .data(recipient)
                .message("Success")
                .build();

        when(keycloakService.existsUserByEmail("john@example.com")).thenReturn(recipientResponse);

        Wallet recipientWallet = new Wallet();
        recipientWallet.setBalance(new BigDecimal("900.00"));
        when(walletRepository.findWalletByCreatedBy("recipient123")).thenReturn(Optional.of(recipientWallet));

        when(accountLimitService.wouldExceedBalanceLimit(eq("recipient123"), any(BigDecimal.class)))
                .thenReturn(true);

        AccountLimitDTO limits = new AccountLimitDTO();
        limits.setMaxWalletBalance(new BigDecimal("950.00"));
        when(accountLimitService.getUserLimits("recipient123")).thenReturn(limits);

        Mono<String> result = service.handleInstantTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(
                        responseText -> responseText.contains("exceed the recipient's wallet balance limit of $950.00")
                                &&
                                responseText.contains("try a smaller amount"))
                .verifyComplete();

        verify(keycloakService).existsUserByEmail("john@example.com");
        verify(walletRepository).findWalletByCreatedBy("recipient123");
        verify(accountLimitService).wouldExceedBalanceLimit(eq("recipient123"), any(BigDecimal.class));
        verify(accountLimitService).getUserLimits("recipient123");
    }

    @Test
    public void testIsValidEmail() {
        // This is testing a private method indirectly through handleRecipientEmailInput
        String validEmail = "test@example.com";
        String invalidEmail = "not-an-email";

        state.setStage(ConversationState.TransactionStage.ENTERING_RECIPIENT_EMAIL);

        // Test with valid email
        when(keycloakService.existsUserByEmail(anyString()))
                .thenReturn(userResponse);
        when(keycloakService.getUserById(userId)).thenReturn(userResponse);

        Mono<String> validResult = service.handleInstantTransferRequest(userId, validEmail, state);

        StepVerifier.create(validResult)
                .expectNextMatches(response -> response.contains("I'm about to transfer"))
                .verifyComplete();

        // Reset state for next test
        state = new ConversationState();
        state.setStage(ConversationState.TransactionStage.ENTERING_RECIPIENT_EMAIL);

        // Test with invalid email
        Mono<String> invalidResult = service.handleInstantTransferRequest(userId, invalidEmail, state);

        StepVerifier.create(invalidResult)
                .expectNext(
                        "That doesn't look like a valid email address. Please enter a valid email, or type 'cancel' to start over:")
                .verifyComplete();
    }
}