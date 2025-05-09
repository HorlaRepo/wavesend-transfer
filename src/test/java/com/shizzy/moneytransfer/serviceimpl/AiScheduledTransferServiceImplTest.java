package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.enums.RecurrenceType;
import com.shizzy.moneytransfer.enums.ScheduleStatus;
import com.shizzy.moneytransfer.model.UserBeneficiaries;
import com.shizzy.moneytransfer.model.UserBeneficiary;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.UserBeneficiariesRepository;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.*;
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
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiScheduledTransferServiceImplTest {

    @Mock
    private ScheduledTransferService scheduledTransferService;

    @Mock
    private AiEntityExtractionService entityExtractionService;

    @Mock
    private ConversationManagerService conversationManager;

    @Mock
    private UserBeneficiariesRepository beneficiaryRepository;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private TransactionLimitService transactionLimitService;

    @Mock
    private AccountLimitService accountLimitService;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private AiScheduledTransferServiceImpl aiScheduledTransferService;

    private final String userId = "user-123";
    private ConversationState state;
    private ScheduledTransferDetails transferDetails;
    private UserRepresentation userRepresentation;
    private ApiResponse<UserRepresentation> userResponse;
    private Wallet userWallet;

    @BeforeEach
    void setUp() {
        state = new ConversationState();

        // Setup transfer details
        transferDetails = new ScheduledTransferDetails();
        transferDetails.setAmount(new BigDecimal("100.00"));
        transferDetails.setRecipientName("John Doe");
        transferDetails.setNote("Test transfer");
        transferDetails.setScheduledDateTime(LocalDateTime.now().plusDays(1));

        // Setup user representation
        userRepresentation = new UserRepresentation();
        userRepresentation.setId(userId);
        userRepresentation.setEmail("user@example.com");

        // Setup user response
        userResponse = new ApiResponse<>();
        userResponse.setSuccess(true);
        userResponse.setData(userRepresentation);

        // Setup wallet
        userWallet = new Wallet();
        userWallet.setBalance(new BigDecimal("1000.00"));
    }

    @Test
    void handleScheduledTransferRequest_InitialFlow_SufficientBalance_OneBeneficiary_Success() {
        // Given
        String message = "schedule a transfer of $100 to John tomorrow";

        when(entityExtractionService.extractScheduledTransferDetails(userId, message))
                .thenReturn(Mono.just(transferDetails));

        when(accountLimitService.wouldExceedTransferLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(accountLimitService.wouldExceedDailyLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);


        when(walletRepository.findWalletByCreatedBy(userId))
                .thenReturn(Optional.of(userWallet));

        // Setup single beneficiary result
        UserBeneficiary beneficiary = new UserBeneficiary();
        beneficiary.setName("John Doe");
        beneficiary.setEmail("john@example.com");
        List<UserBeneficiary> beneficiaries = List.of(beneficiary);

        when(beneficiaryRepository.findByUserIdWithBeneficiaries(userId))
                .thenReturn(List.of(new UserBeneficiaries(userId, beneficiaries)));

        when(keycloakService.existsUserByEmail(beneficiary.getEmail()))
                .thenReturn(userResponse);

        when(keycloakService.getUserById(userId))
                .thenReturn(userResponse);

        // When & Then
        Mono<String> result = aiScheduledTransferService.handleScheduledTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("I'm about to schedule a transfer of $100.00") &&
                        response.contains("to John Doe"))
                .verifyComplete();

        // Verify state was updated correctly
        assertEquals(ConversationState.TransactionStage.CONFIRMING_TRANSACTION, state.getStage());
        assertEquals(beneficiary.getEmail(), state.getSelectedBeneficiaryEmail());
        assertEquals(transferDetails.getAmount(), state.getAmount());
    }

    @Test
    void handleOngoingFlow_SelectingBeneficiary_ValidSelection_Success() {
        // Given
        String message = "1"; // Selecting the first beneficiary
        state.setStage(ConversationState.TransactionStage.SELECTING_BENEFICIARY);
        state.setAmount(new BigDecimal("100.00"));
        state.setScheduledDateTime(LocalDateTime.now().plusDays(1));

        // Setup matching beneficiaries
        UserBeneficiary beneficiary1 = new UserBeneficiary();
        beneficiary1.setName("John Doe");
        beneficiary1.setEmail("john@example.com");

        UserBeneficiary beneficiary2 = new UserBeneficiary();
        beneficiary2.setName("Jane Doe");
        beneficiary2.setEmail("jane@example.com");

        List<UserBeneficiary> beneficiaries = new ArrayList<>();
        beneficiaries.add(beneficiary1);
        beneficiaries.add(beneficiary2);

        state.setMatchingBeneficiaries(beneficiaries);

        when(keycloakService.getUserById(userId))
                .thenReturn(userResponse);

        // When & Then
        Mono<String> result = aiScheduledTransferService.handleScheduledTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("I'm about to schedule a transfer") &&
                        response.contains("to John Doe"))
                .verifyComplete();

        // Verify state was updated correctly
        assertEquals(ConversationState.TransactionStage.CONFIRMING_TRANSACTION, state.getStage());
        assertEquals(beneficiary1.getEmail(), state.getSelectedBeneficiaryEmail());
    }

    @Test
    void handleOngoingFlow_ConfirmingTransaction_UserConfirms_Success() {
        // Given
        String message = "yes"; // User confirms
        state.setStage(ConversationState.TransactionStage.CONFIRMING_TRANSACTION);
        state.setAmount(new BigDecimal("100.00"));
        state.setSelectedBeneficiaryEmail("john@example.com");
        state.setScheduledDateTime(LocalDateTime.now().plusDays(1));

        when(walletRepository.findWalletByCreatedBy(userId))
                .thenReturn(Optional.of(userWallet));

        when(keycloakService.getUserById(userId))
                .thenReturn(userResponse);

        // Setup scheduled transfer initiation response
        ScheduledTransferInitiationResponse initiationResponse = new ScheduledTransferInitiationResponse();
        initiationResponse.setScheduledTransferToken("token-123");

        ApiResponse<ScheduledTransferInitiationResponse> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setData(initiationResponse);

        when(scheduledTransferService.initiateScheduledTransfer(any(ScheduledTransferRequestDTO.class), eq(userId)))
                .thenReturn(apiResponse);

        // When & Then
        Mono<String> result = aiScheduledTransferService.handleScheduledTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("I've initiated your scheduled transfer") &&
                        response.contains("verification code"))
                .verifyComplete();

        // Verify state was updated correctly
        assertEquals(ConversationState.TransactionStage.ENTERING_OTP, state.getStage());
        assertEquals("token-123", state.getScheduledTransferToken());
        assertNotNull(state.getOtpSentAt());
    }

    @Test
    void handleOngoingFlow_EnteringOTP_ValidOTP_Success() {
        // Given
        String message = "123456"; // OTP code
        state.setStage(ConversationState.TransactionStage.ENTERING_OTP);
        state.setAmount(new BigDecimal("100.00"));
        state.setSelectedBeneficiaryEmail("john@example.com");
        state.setScheduledDateTime(LocalDateTime.now().plusDays(1));
        state.setScheduledTransferToken("token-123");
        state.setRecipientName("John Doe");

        // Setup scheduled transfer response
        ScheduledTransferResponseDTO responseDTO = new ScheduledTransferResponseDTO(
                1L, // id
                userRepresentation.getEmail(), // senderEmail
                "john@example.com", // receiverEmail
                new BigDecimal("100.00"), // amount
                LocalDateTime.now().plusDays(1), // scheduledDateTime
                "Test transfer", // description
                ScheduleStatus.PENDING, // status
                LocalDateTime.now(), // createdAt
                RecurrenceType.NONE, // recurrenceType
                null, // recurrenceEndDate
                null, // totalOccurrences
                null, // currentOccurrence
                null, // parentTransferId
                false // isRecurring
        );

        ApiResponse<ScheduledTransferResponseDTO> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setData(responseDTO);

        when(scheduledTransferService.verifyAndScheduleTransfer(any(ScheduledTransferVerificationRequest.class),
                eq(userId)))
                .thenReturn(apiResponse);

        // When & Then
        Mono<String> result = aiScheduledTransferService.handleScheduledTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("Success!") &&
                        response.contains("Your transfer of $100.00 to John Doe"))
                .verifyComplete();

        // Verify state was updated correctly
        assertEquals(ConversationState.TransactionStage.TRANSACTION_COMPLETED, state.getStage());
        verify(accountLimitService).recordTransaction(eq(userId), eq(new BigDecimal("100.00")));
    }

    @Test
    void handleScheduledTransferRequest_InitialFlow_InsufficientBalance() {
        // Given
        String message = "schedule a transfer of $2000 to John tomorrow";
        transferDetails.setAmount(new BigDecimal("2000.00")); // More than wallet balance

        when(entityExtractionService.extractScheduledTransferDetails(userId, message))
                .thenReturn(Mono.just(transferDetails));

        when(accountLimitService.wouldExceedTransferLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(accountLimitService.wouldExceedDailyLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);

        when(walletRepository.findWalletByCreatedBy(userId))
                .thenReturn(Optional.of(userWallet)); // Balance is 1000.00

        // When & Then
        Mono<String> result = aiScheduledTransferService.handleScheduledTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("don't have enough balance") &&
                        response.contains("Would you like to try a different amount?"))
                .verifyComplete();
    }

    @Test
    void handleScheduledTransferRequest_InitialFlow_TransferLimitExceeded() {
        // Given
        String message = "schedule a transfer of $500 to John tomorrow";

        when(entityExtractionService.extractScheduledTransferDetails(userId, message))
                .thenReturn(Mono.just(transferDetails));

        when(accountLimitService.wouldExceedTransferLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(true);

                AccountLimitDTO limits = AccountLimitDTO.builder()
                .maxTransferAmount(new BigDecimal("50"))        // Max transfer amount - lower than requested
                .dailyTransactionLimit(new BigDecimal("1000"))  // Daily transaction limit
                .maxWalletBalance(new BigDecimal("5000"))       // Max wallet balance
                .build();

        when(accountLimitService.getUserLimits(userId))
                .thenReturn(limits);

        // When & Then
        Mono<String> result = aiScheduledTransferService.handleScheduledTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("exceeds your transfer limit") &&
                        response.contains("Would you like to try a different amount?"))
                .verifyComplete();
    }

    @Test
    void handleScheduledTransferRequest_InitialFlow_NoBeneficiariesFound() {
        // Given
        String message = "schedule a transfer of $100 to John tomorrow";

        when(entityExtractionService.extractScheduledTransferDetails(userId, message))
                .thenReturn(Mono.just(transferDetails));

        when(accountLimitService.wouldExceedTransferLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);
        when(accountLimitService.wouldExceedDailyLimit(anyString(), any(BigDecimal.class)))
                .thenReturn(false);

        when(walletRepository.findWalletByCreatedBy(userId))
                .thenReturn(Optional.of(userWallet));

        // Return empty list of beneficiaries
        when(beneficiaryRepository.findByUserIdWithBeneficiaries(userId))
                .thenReturn(List.of());

        // When & Then
        Mono<String> result = aiScheduledTransferService.handleScheduledTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("I couldn't find anyone named") &&
                        response.contains("Please add them as a beneficiary first"))
                .verifyComplete();
    }

    @Test
    void handleOngoingFlow_ConfirmingTransaction_UserCancels() {
        // Given
        String message = "no"; // User cancels
        state.setStage(ConversationState.TransactionStage.CONFIRMING_TRANSACTION);

        // When & Then
        Mono<String> result = aiScheduledTransferService.handleScheduledTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.contains("I've cancelled the scheduled transfer"))
                .verifyComplete();

        // Verify state was reset
        assertEquals(ConversationState.TransactionStage.NONE, state.getStage());
    }

    @Test
    void handleOngoingFlow_EnteringOTP_TooManyFailedAttempts() {
        // Given
        String message = "123456"; // OTP code
        state.setStage(ConversationState.TransactionStage.ENTERING_OTP);
        state.setScheduledTransferToken("token-123");
        // Set OTP attempts to max
        for (int i = 0; i < 3; i++) {
            state.incrementOtpAttempts();
        }

        // OTP verification fails
        ApiResponse<ScheduledTransferResponseDTO> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(false);
        apiResponse.setMessage("Invalid OTP");

        when(scheduledTransferService.verifyAndScheduleTransfer(any(ScheduledTransferVerificationRequest.class),
                eq(userId)))
                .thenReturn(apiResponse);

        // When & Then
        Mono<String> result = aiScheduledTransferService.handleScheduledTransferRequest(userId, message, state);

        StepVerifier.create(result)
                .expectNextMatches(response -> response
                        .contains("Sorry, you've reached the maximum number of verification attempts"))
                .verifyComplete();

        // Verify state was updated correctly
        assertEquals(ConversationState.TransactionStage.TRANSACTION_FAILED, state.getStage());
    }
}