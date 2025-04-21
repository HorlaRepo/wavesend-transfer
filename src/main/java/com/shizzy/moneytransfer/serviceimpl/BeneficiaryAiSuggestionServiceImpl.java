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
import com.shizzy.moneytransfer.service.BeneficiaryAiSuggestionService;
import com.shizzy.moneytransfer.service.KeycloakService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiaryAiSuggestionServiceImpl implements BeneficiaryAiSuggestionService {

    private final OpenRouterAiClient openRouterAiClient;
    private final UserBeneficiariesRepository beneficiariesRepository;
    private final WalletRepository walletRepository;
    private final BeneficiaryAiSuggestionRepository suggestionRepository;
    private final KeycloakService keycloakService;

    @Value("${app.beneficiary.low-balance-threshold}")
    private BigDecimal lowBalanceThreshold;

    @Value("${app.beneficiary.user-min-balance}")
    private BigDecimal userMinBalance;

    @Async("aiTaskExecutor")
    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Void> generateSuggestionsForUser(String userId) {
        log.info("Generating AI suggestions for user: {}", userId);

        try {
            // Get user wallet
            Optional<Wallet> userWalletOpt = walletRepository.findWalletByCreatedBy(userId);
            if (userWalletOpt.isEmpty()) {
                log.info("User {} has no wallet, skipping suggestion generation", userId);
                return CompletableFuture.completedFuture(null);
            }

            Wallet userWallet = userWalletOpt.get();
            BigDecimal userBalance = userWallet.getBalance();

            // Check if user has enough balance to help others
            if (userBalance.compareTo(userMinBalance) < 0) {
                log.info("User {} has insufficient balance ({}) to help others",
                        userId, userBalance);
                return CompletableFuture.completedFuture(null);
            }

            // Get user beneficiaries
            Optional<UserBeneficiaries> userBeneficiariesOpt = beneficiariesRepository
                    .findByIdWithBeneficiaries(userId);
            if (userBeneficiariesOpt.isEmpty()) {
                log.info("User {} has no beneficiaries, skipping suggestion generation", userId);
                return CompletableFuture.completedFuture(null);
            }

            UserBeneficiaries userBeneficiaries = userBeneficiariesOpt.get();

            List<UserBeneficiary> beneficiaryList = new ArrayList<>(userBeneficiaries.getBeneficiaries());

            if (beneficiaryList.isEmpty()) {
                log.info("User {} has no beneficiaries, skipping suggestion generation", userId);
                return CompletableFuture.completedFuture(null);
            }

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // Check each beneficiary's balance
            for (UserBeneficiary beneficiary : beneficiaryList) {
                CompletableFuture<Void> future = checkBeneficiaryAndGenerateSuggestion(
                        userId, userBalance, beneficiary);
                futures.add(future);
            }

            // Wait for all futures to complete
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        } catch (Exception e) {
            log.error("Error generating suggestions for user {}", userId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Void> checkBeneficiaryAndGenerateSuggestion(
            String userId, BigDecimal userBalance, UserBeneficiary beneficiary) {

        return CompletableFuture.runAsync(() -> {
            try {
                // Check if beneficiary has wallet
                UserRepresentation beneficiaryUser = keycloakService.existsUserByEmail(beneficiary.getEmail())
                        .getData();

                Optional<Wallet> beneficiaryWalletOpt = walletRepository.findWalletByCreatedBy(beneficiaryUser.getId());
                if (beneficiaryWalletOpt.isEmpty()) {
                    return;
                }

                Wallet beneficiaryWallet = beneficiaryWalletOpt.get();
                BigDecimal beneficiaryBalance = beneficiaryWallet.getBalance();

                // Check if beneficiary has low balance
                if (beneficiaryBalance.compareTo(lowBalanceThreshold) < 0) {
                    LocalDateTime now = LocalDateTime.now();

                    // Check if we already have a recent suggestion for this beneficiary
                    boolean hasActiveSuggestion = suggestionRepository
                            .existsByUserIdAndBeneficiaryIdAndExpiresAtAfter(
                                    userId, beneficiary.getId(), now);

                    if (!hasActiveSuggestion) {
                        // Calculate suggested amount (10% of user balance or $10, whichever is less)
                        BigDecimal tenPercent = userBalance.multiply(new BigDecimal("0.1"));
                        BigDecimal suggestedAmount = tenPercent.min(new BigDecimal("10.00"));

                        // Keep suggested amount reasonable
                        if (suggestedAmount.compareTo(new BigDecimal("5.00")) < 0) {
                            suggestedAmount = new BigDecimal("5.00");
                        } else if (suggestedAmount.compareTo(new BigDecimal("20.00")) > 0) {
                            suggestedAmount = new BigDecimal("20.00");
                        }

                        // Round to 2 decimal places
                        suggestedAmount = suggestedAmount.setScale(2, java.math.RoundingMode.HALF_UP);

                        // Determine relationship (just use "friend" for now)
                        String relationship = "friend";

                        String userFirstName = keycloakService.getUserById(userId).getData()
                                .getFirstName();

                        // Get the suggestion from AI
                        String suggestionText = openRouterAiClient.generateBeneficiarySuggestion(
                                userFirstName, beneficiary.getName(), userBalance,
                                beneficiaryBalance, relationship)
                                .block(java.time.Duration.ofSeconds(10));

                        // Create and save the suggestion
                        BeneficiaryAiSuggestion suggestion = BeneficiaryAiSuggestion.builder()
                                .userId(userId)
                                .beneficiaryId(beneficiary.getId())
                                .beneficiaryName(beneficiary.getName())
                                .suggestedAmount(suggestedAmount)
                                .suggestionText(suggestionText)
                                .expiresAt(now.plusDays(1))
                                .seen(false)
                                .dismissed(false)
                                .build();

                        suggestionRepository.save(suggestion);
                        log.info("Generated suggestion for user {} to help beneficiary {}",
                                userId, beneficiary.getEmail());
                    }
                }
            } catch (Exception e) {
                log.error("Error generating suggestion for beneficiary {}", beneficiary.getEmail(), e);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<BeneficiaryAiSuggestion>> getUserSuggestions(String userId) {
        List<BeneficiaryAiSuggestion> suggestions = suggestionRepository
                .findByUserIdAndDismissedFalseAndExpiresAtAfter(userId, LocalDateTime.now());

        // Mark suggestions as seen
        suggestions.forEach(suggestion -> suggestion.setSeen(true));
        suggestionRepository.saveAll(suggestions);

        return ApiResponse.<List<BeneficiaryAiSuggestion>>builder()
                .success(true)
                .data(suggestions)
                .message(suggestions.isEmpty() ? "No suggestions found"
                        : "Retrieved " + suggestions.size() + " suggestions")
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<String> dismissSuggestion(String userId, Long suggestionId) {
        BeneficiaryAiSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Suggestion not found"));

        // Verify ownership
        if (!suggestion.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You cannot dismiss a suggestion that doesn't belong to you");
        }

        suggestion.setDismissed(true);
        suggestionRepository.save(suggestion);

        return ApiResponse.<String>builder()
                .success(true)
                .data("Suggestion dismissed successfully")
                .message("Suggestion dismissed successfully")
                .build();
    }

    @Override
    @Scheduled(fixedRate = 86400000) // Daily cleanup
    @Transactional
    public void deleteExpiredSuggestions() {
        LocalDateTime now = LocalDateTime.now();
        suggestionRepository.deleteByExpiresAtBefore(now);
        log.info("Deleted expired suggestions");
    }

    // Scheduled job to check all users periodically for suggestions
    @Scheduled(fixedRate = 3600000) // Every hour
    public void generateSuggestionsForAllUsers() {
        log.info("Starting scheduled suggestion generation for all users");

        List<String> usersWithBeneficiaries = beneficiariesRepository.findAll()
                .stream()
                .map(UserBeneficiaries::getUserId)
                .toList();

        log.info("Found {} users with beneficiaries", usersWithBeneficiaries.size());

        for (String userId : usersWithBeneficiaries) {
            generateSuggestionsForUser(userId)
                    .orTimeout(30, TimeUnit.SECONDS)
                    .exceptionally(throwable -> {
                        log.error("Error generating suggestions for user {}", userId, throwable);
                        return null;
                    });

            try {
                // Brief pause between users to avoid overwhelming external services
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Suggestion generation interrupted", e);
                break;
            }
        }
    }
}