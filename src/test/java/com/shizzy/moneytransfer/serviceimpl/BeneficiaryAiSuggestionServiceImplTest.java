package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.client.OpenRouterAiClient;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.BeneficiaryAiSuggestion;
import com.shizzy.moneytransfer.model.UserBeneficiaries;
import com.shizzy.moneytransfer.model.UserBeneficiary;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.BeneficiaryAiSuggestionRepository;
import com.shizzy.moneytransfer.repository.UserBeneficiariesRepository;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.KeycloakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryAiSuggestionServiceImplTest {

    @Mock
    private OpenRouterAiClient openRouterAiClient;

    @Mock
    private UserBeneficiariesRepository beneficiariesRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private BeneficiaryAiSuggestionRepository suggestionRepository;

    @Mock
    private KeycloakService keycloakService;

    @InjectMocks
    private BeneficiaryAiSuggestionServiceImpl beneficiaryAiSuggestionService;

    private final String USER_ID = "user-123";
    private final String RECEIVER_ID = "receiver-123";
    private final Long BENEFICIARY_ID = 1L;
    private final String BENEFICIARY_EMAIL = "beneficiary@example.com";
    private final String BENEFICIARY_NAME = "John Doe";
    private final String USER_FIRST_NAME = "Alice";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(beneficiaryAiSuggestionService, "lowBalanceThreshold", new BigDecimal("50.00"));
        ReflectionTestUtils.setField(beneficiaryAiSuggestionService, "userMinBalance", new BigDecimal("100.00"));
    }

    @Test
    void generateSuggestionsForUser_NoWallet_ReturnsCompletedFuture() {
        when(walletRepository.findWalletByCreatedBy(USER_ID)).thenReturn(Optional.empty());

        CompletableFuture<Void> result = beneficiaryAiSuggestionService.generateSuggestionsForUser(USER_ID);

        assertNotNull(result);
        assertTrue(result.isDone());
        verify(walletRepository).findWalletByCreatedBy(USER_ID);
        verifyNoMoreInteractions(beneficiariesRepository, suggestionRepository, keycloakService, openRouterAiClient);
    }

    @Test
    void generateSuggestionsForUser_InsufficientBalance_ReturnsCompletedFuture() {
        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("50.00")); // Less than userMinBalance

        when(walletRepository.findWalletByCreatedBy(USER_ID)).thenReturn(Optional.of(wallet));

        CompletableFuture<Void> result = beneficiaryAiSuggestionService.generateSuggestionsForUser(USER_ID);

        assertNotNull(result);
        assertTrue(result.isDone());
        verify(walletRepository).findWalletByCreatedBy(USER_ID);
        verifyNoMoreInteractions(beneficiariesRepository, suggestionRepository, keycloakService, openRouterAiClient);
    }

    @Test
    void generateSuggestionsForUser_WithBeneficiaries_ProcessesAllBeneficiaries() throws Exception {
        Wallet userWallet = new Wallet();
        userWallet.setBalance(new BigDecimal("200.00"));

        UserBeneficiary beneficiary = new UserBeneficiary();
        beneficiary.setId(BENEFICIARY_ID);
        beneficiary.setEmail(BENEFICIARY_EMAIL);
        beneficiary.setName(BENEFICIARY_NAME);

        List<UserBeneficiary> beneficiaryList = Collections.singletonList(beneficiary);

        UserBeneficiaries userBeneficiaries = new UserBeneficiaries();
        userBeneficiaries.setUserId(USER_ID);
        userBeneficiaries.setBeneficiaries(beneficiaryList);

        UserRepresentation beneficiaryUser = new UserRepresentation();
        beneficiaryUser.setId(RECEIVER_ID); 

        Wallet beneficiaryWallet = new Wallet();
        beneficiaryWallet.setBalance(new BigDecimal("20.00"));

        ApiResponse<UserRepresentation> userExistsResponse = ApiResponse.<UserRepresentation>builder()
                .success(true)
                .data(beneficiaryUser)
                .build();

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setFirstName(USER_FIRST_NAME);

        ApiResponse<UserRepresentation> userByIdResponse = ApiResponse.<UserRepresentation>builder()
                .success(true)
                .data(userRepresentation)
                .build();

        when(walletRepository.findWalletByCreatedBy(USER_ID)).thenReturn(Optional.of(userWallet));
        // Fix the stubbing to use USER_ID instead of RECEIVER_ID
        when(beneficiariesRepository.findByIdWithBeneficiaries(USER_ID)).thenReturn(Optional.of(userBeneficiaries));
        when(keycloakService.existsUserByEmail(BENEFICIARY_EMAIL)).thenReturn(userExistsResponse);
        when(walletRepository.findWalletByCreatedBy(RECEIVER_ID)).thenReturn(Optional.of(beneficiaryWallet));
        when(suggestionRepository.existsByUserIdAndBeneficiaryIdAndExpiresAtAfter(eq(USER_ID), eq(BENEFICIARY_ID),
                any(LocalDateTime.class)))
                .thenReturn(false);
        when(keycloakService.getUserById(USER_ID)).thenReturn(userByIdResponse);
        when(openRouterAiClient.generateBeneficiarySuggestion(
                eq(USER_FIRST_NAME), eq(BENEFICIARY_NAME),
                any(BigDecimal.class), any(BigDecimal.class), eq("friend")))
                .thenReturn(Mono.just("Here's a suggestion text"));
        when(suggestionRepository.save(any(BeneficiaryAiSuggestion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<Void> result = beneficiaryAiSuggestionService.generateSuggestionsForUser(USER_ID);
        result.join();

        verify(walletRepository).findWalletByCreatedBy(USER_ID);
        verify(beneficiariesRepository).findByIdWithBeneficiaries(USER_ID);
        verify(keycloakService).existsUserByEmail(BENEFICIARY_EMAIL);
        verify(walletRepository).findWalletByCreatedBy(RECEIVER_ID);
        verify(suggestionRepository).existsByUserIdAndBeneficiaryIdAndExpiresAtAfter(eq(USER_ID), eq(BENEFICIARY_ID),
                any(LocalDateTime.class));
        verify(keycloakService).getUserById(USER_ID);
        verify(openRouterAiClient).generateBeneficiarySuggestion(
                eq(USER_FIRST_NAME), eq(BENEFICIARY_NAME),
                any(BigDecimal.class), any(BigDecimal.class), eq("friend"));
        verify(suggestionRepository).save(any(BeneficiaryAiSuggestion.class));
    }

    @Test
    void getUserSuggestions_NoSuggestions_ReturnsEmptyList() {
        when(suggestionRepository.findByUserIdAndDismissedFalseAndExpiresAtAfter(eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        ApiResponse<List<BeneficiaryAiSuggestion>> response = beneficiaryAiSuggestionService
                .getUserSuggestions(USER_ID);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("No suggestions found", response.getMessage());
        assertTrue(response.getData().isEmpty());
        verify(suggestionRepository).findByUserIdAndDismissedFalseAndExpiresAtAfter(eq(USER_ID),
                any(LocalDateTime.class));
        verify(suggestionRepository).saveAll(Collections.emptyList());
    }

    @Test
    void getUserSuggestions_WithSuggestions_ReturnsListAndMarksSeen() {
        BeneficiaryAiSuggestion suggestion = BeneficiaryAiSuggestion.builder()
                .userId(USER_ID)
                .beneficiaryId(BENEFICIARY_ID)
                .beneficiaryName(BENEFICIARY_NAME)
                .seen(false)
                .build();

        List<BeneficiaryAiSuggestion> suggestions = Collections.singletonList(suggestion);

        when(suggestionRepository.findByUserIdAndDismissedFalseAndExpiresAtAfter(eq(USER_ID), any(LocalDateTime.class)))
                .thenReturn(suggestions);
        when(suggestionRepository.saveAll(anyList())).thenReturn(suggestions);

        ApiResponse<List<BeneficiaryAiSuggestion>> response = beneficiaryAiSuggestionService
                .getUserSuggestions(USER_ID);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Retrieved 1 suggestions", response.getMessage());
        assertEquals(1, response.getData().size());
        assertTrue(response.getData().get(0).isSeen());
        verify(suggestionRepository).findByUserIdAndDismissedFalseAndExpiresAtAfter(eq(USER_ID),
                any(LocalDateTime.class));
        verify(suggestionRepository).saveAll(anyList());
    }

    @Test
    void dismissSuggestion_SuggestionNotFound_ThrowsException() {
        Long suggestionId = 123L;
        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> beneficiaryAiSuggestionService.dismissSuggestion(USER_ID, suggestionId));

        verify(suggestionRepository).findById(suggestionId);
        verifyNoMoreInteractions(suggestionRepository);
    }

    @Test
    void dismissSuggestion_NotOwnedBySuggestion_ThrowsException() {
        Long suggestionId = 123L;
        String differentUserId = "different-user";

        BeneficiaryAiSuggestion suggestion = BeneficiaryAiSuggestion.builder()
                .userId(differentUserId)
                .beneficiaryId(BENEFICIARY_ID)
                .build();

        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.of(suggestion));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> beneficiaryAiSuggestionService.dismissSuggestion(USER_ID, suggestionId));

        assertEquals("You cannot dismiss a suggestion that doesn't belong to you", exception.getMessage());
        verify(suggestionRepository).findById(suggestionId);
        verifyNoMoreInteractions(suggestionRepository);
    }

    @Test
    void dismissSuggestion_ValidSuggestion_SuccessfullyDismisses() {
        Long suggestionId = 123L;

        BeneficiaryAiSuggestion suggestion = BeneficiaryAiSuggestion.builder()
                .userId(USER_ID)
                .beneficiaryId(BENEFICIARY_ID)
                .dismissed(false)
                .build();

        when(suggestionRepository.findById(suggestionId)).thenReturn(Optional.of(suggestion));
        when(suggestionRepository.save(any(BeneficiaryAiSuggestion.class))).thenReturn(suggestion);

        ApiResponse<String> response = beneficiaryAiSuggestionService.dismissSuggestion(USER_ID, suggestionId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Suggestion dismissed successfully", response.getMessage());
        assertEquals("Suggestion dismissed successfully", response.getData());
        assertTrue(suggestion.isDismissed());
        verify(suggestionRepository).findById(suggestionId);
        verify(suggestionRepository).save(suggestion);
    }

    @Test
    void deleteExpiredSuggestions_DeletesFromRepository() {
        beneficiaryAiSuggestionService.deleteExpiredSuggestions();

        verify(suggestionRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }

    @Test
    void generateSuggestionsForAllUsers_ProcessesAllUsers() {
        List<String> userIds = List.of("user1", "user2", "user3");

        List<UserBeneficiaries> userBeneficiaries = new ArrayList<>();
        for (String userId : userIds) {
            UserBeneficiaries ub = new UserBeneficiaries();
            ub.setUserId(userId);
            userBeneficiaries.add(ub);
        }

        when(beneficiariesRepository.findAll()).thenReturn(userBeneficiaries);
        when(walletRepository.findWalletByCreatedBy(anyString())).thenReturn(Optional.empty());

        beneficiaryAiSuggestionService.generateSuggestionsForAllUsers();

        verify(beneficiariesRepository).findAll();
        for (String userId : userIds) {
            verify(walletRepository).findWalletByCreatedBy(userId);
        }
    }
}