package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.enums.RecurrenceType;
import com.shizzy.moneytransfer.exception.TransactionLimitExceededException;
import com.shizzy.moneytransfer.model.UserBeneficiaries;
import com.shizzy.moneytransfer.model.UserBeneficiary;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.UserBeneficiariesRepository;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiScheduledTransferServiceImpl {

    private final ScheduledTransferService scheduledTransferService;
    private final AiEntityExtractionService entityExtractionService;
    private final ConversationManagerService conversationManager;
    private final UserBeneficiariesRepository beneficiaryRepository;
    private final KeycloakService keycloakService;
    private final TransactionLimitService transactionLimitService;
    private final AccountLimitService accountLimitService;
    private final WalletRepository walletRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a");

    private static final Duration OTP_RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final int MAX_OTP_ATTEMPTS = 3;

    public Mono<String> handleScheduledTransferRequest(String userId, String message, ConversationState state) {
        // If we're in the middle of a flow, continue that flow
        if (state.getStage() != ConversationState.TransactionStage.NONE) {
            return handleOngoingFlow(userId, message, state);
        }

        // Extract scheduled transfer details from message
        return entityExtractionService.extractScheduledTransferDetails(userId, message)
                .flatMap(details -> {
                    log.info("Extracted scheduled transfer details: {}", details);

                    // Store extracted details in conversation state
                    state.setAmount(details.getAmount());
                    state.setRecipientName(details.getRecipientName());
                    state.setNote(details.getNote());
                    state.setScheduledDateTime(details.getScheduledDateTime());

                    // Pre-validate transaction limits before proceeding
                    return preValidateTransactionLimits(userId, details.getAmount())
                            .flatMap(limitValidationResult -> {
                                // If validation failed, return error message immediately
                                if (!limitValidationResult.isSuccess()) {
                                    return Mono.just(limitValidationResult.getMessage());
                                }

                                // Check wallet balance first
                                try {
                                    BigDecimal balance = getUserBalance(userId);

                                    // Check if user has enough balance
                                    if (balance.compareTo(details.getAmount()) < 0) {
                                        return Mono.just(
                                                "I'm sorry, but you don't have enough balance for this transfer. " +
                                                        "Your current balance is $" + balance
                                                        + " but you're trying to send $" +
                                                        details.getAmount()
                                                        + ". Would you like to try a different amount?");
                                    }
                                } catch (Exception e) {
                                    log.error("Error checking user balance", e);
                                    // Continue with flow - we'll check balance again during actual transfer
                                }

                                // Find matching beneficiaries
                                return findBeneficiaries(userId, details.getRecipientName())
                                        .flatMap(beneficiaries -> {
                                            if (beneficiaries.isEmpty()) {
                                                // No matching beneficiaries found
                                                return Mono.just("I couldn't find anyone named \""
                                                        + details.getRecipientName() +
                                                        "\" in your beneficiaries. Please add them as a beneficiary first, "
                                                        +
                                                        "or try with a different name.");
                                            } else if (beneficiaries.size() == 1) {
                                                // One match found, confirm details with user
                                                UserBeneficiary beneficiary = beneficiaries.get(0);
                                                state.setSelectedBeneficiaryEmail(beneficiary.getEmail());

                                                // Validate recipient limits
                                                return validateRecipientLimits(beneficiary.getEmail(),
                                                        state.getAmount())
                                                        .flatMap(recipientValidationResult -> {
                                                            // If validation failed, return error message and reset
                                                            // state
                                                            if (!recipientValidationResult.isSuccess()) {
                                                                state.reset();
                                                                return Mono
                                                                        .just(recipientValidationResult.getMessage());
                                                            }

                                                            state.setStage(
                                                                    ConversationState.TransactionStage.CONFIRMING_TRANSACTION);

                                                            return Mono.just(keycloakService.getUserById(userId))
                                                                    .map(userResponse -> {
                                                                        UserRepresentation user = userResponse
                                                                                .getData();
                                                                        return formatConfirmationMessage(
                                                                                details,
                                                                                beneficiary.getName(),
                                                                                user.getEmail());
                                                                    });
                                                        });

                                            } else {
                                                // Multiple matches found, ask user to select
                                                state.setMatchingBeneficiaries(beneficiaries);
                                                state.setStage(
                                                        ConversationState.TransactionStage.SELECTING_BENEFICIARY);

                                                return Mono.just(formatBeneficiarySelectionMessage(beneficiaries));
                                            }
                                        });
                            });
                });
    }

    @Transactional(readOnly = true)
    private Mono<String> handleOngoingFlow(String userId, String message, ConversationState state) {
        switch (state.getStage()) {
            case SELECTING_BENEFICIARY:
                return handleBeneficiarySelection(userId, message, state);

            case CONFIRMING_TRANSACTION:
                return handleConfirmation(userId, message, state);

            case ENTERING_OTP:
                return handleOtpVerification(userId, message, state);

            default:
                // Reset state and generate a general response
                state.reset();
                return conversationManager.generateContextualResponse(userId, message);
        }
    }

    @Transactional(readOnly = true)
    private Mono<String> handleBeneficiarySelection(String userId, String message, ConversationState state) {
        try {
            int selection = extractSelectionNumber(message);
            List<UserBeneficiary> beneficiaries = state.getMatchingBeneficiaries();

            if (selection < 1 || selection > beneficiaries.size()) {
                return Mono.just("Please select a valid option between 1 and " + beneficiaries.size());
            }

            UserBeneficiary selected = beneficiaries.get(selection - 1);

            // Validate recipient limits
            return validateRecipientLimits(selected.getEmail(), state.getAmount())
                    .flatMap(limitValidationResult -> {
                        // If validation failed, return error message and reset state
                        if (!limitValidationResult.isSuccess()) {
                            state.reset();
                            return Mono.just(limitValidationResult.getMessage());
                        }

                        state.setSelectedBeneficiaryEmail(selected.getEmail());
                        state.setStage(ConversationState.TransactionStage.CONFIRMING_TRANSACTION);

                        // Get user details directly
                        ApiResponse<UserRepresentation> userResponse = keycloakService.getUserById(userId);
                        UserRepresentation user = userResponse.getData();

                        ScheduledTransferDetails details = new ScheduledTransferDetails();
                        details.setAmount(state.getAmount());
                        details.setScheduledDateTime(state.getScheduledDateTime());
                        details.setNote(state.getNote());

                        return Mono.just(formatConfirmationMessage(
                                details,
                                selected.getName(),
                                user.getEmail()));
                    });
        } catch (NumberFormatException e) {
            // Try to match by name instead
            String name = message.trim();
            List<UserBeneficiary> matches = state.getMatchingBeneficiaries().stream()
                    .filter(b -> b.getName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());

            if (matches.size() == 1) {
                UserBeneficiary selected = matches.get(0);

                // Validate recipient limits
                return validateRecipientLimits(selected.getEmail(), state.getAmount())
                        .flatMap(limitValidationResult -> {
                            // If validation failed, return error message and reset state
                            if (!limitValidationResult.isSuccess()) {
                                state.reset();
                                return Mono.just(limitValidationResult.getMessage());
                            }

                            state.setSelectedBeneficiaryEmail(selected.getEmail());
                            state.setStage(ConversationState.TransactionStage.CONFIRMING_TRANSACTION);

                            // Get user details directly
                            ApiResponse<UserRepresentation> userResponse = keycloakService.getUserById(userId);
                            UserRepresentation user = userResponse.getData();

                            ScheduledTransferDetails details = new ScheduledTransferDetails();
                            details.setAmount(state.getAmount());
                            details.setScheduledDateTime(state.getScheduledDateTime());
                            details.setNote(state.getNote());

                            return Mono.just(formatConfirmationMessage(
                                    details,
                                    selected.getName(),
                                    user.getEmail()));
                        });
            } else {
                return Mono
                        .just("I'm not sure which person you meant. Please enter just the number next to their name.");
            }
        }

    }

    private Mono<String> handleConfirmation(String userId, String message, ConversationState state) {
        // Check if user confirmed
        if (isPositiveResponse(message)) {
            // Initiate scheduled transfer
            return initiateScheduledTransfer(userId, state);
        } else if (isNegativeResponse(message)) {
            // User declined, reset
            state.reset();
            return Mono.just("I've cancelled the scheduled transfer. Is there anything else you'd like to do?");
        } else {
            // Unclear response
            return Mono.just(
                    "Please confirm if you want to proceed with this scheduled transfer. Say yes to proceed or no to cancel.");
        }
    }

    private Mono<String> initiateScheduledTransfer(String userId, ConversationState state) {
        // Final validation of transaction limits
        try {
            // Validate transfer limit
            transactionLimitService.validateTransfer(userId, state.getAmount());

            // Check if we have the balance at the moment (note: future balance may differ)
            BigDecimal balance = getUserBalance(userId);
            if (balance.compareTo(state.getAmount()) < 0) {
                return Mono.just("I'm sorry, but you currently don't have enough balance for this transfer. " +
                        "Your current balance is $" + balance + " but you're trying to send $" +
                        state.getAmount() + ". Please ensure you have sufficient funds by the scheduled date.");
            }
        } catch (TransactionLimitExceededException e) {
            return Mono.just("I'm sorry, but " + e.getMessage() +
                    ". Would you like to try a different amount?");
        } catch (Exception e) {
            // Log but continue - let the service handle it
            log.warn("Error during final validation (will attempt anyway): {}", e.getMessage());
        }

        // Get user email
        return Mono.just(keycloakService.getUserById(userId))
                .flatMap(userResponse -> {
                    UserRepresentation user = userResponse.getData();

                    // Create request
                    ScheduledTransferRequestDTO request = new ScheduledTransferRequestDTO(
                            user.getEmail(),
                            state.getSelectedBeneficiaryEmail(),
                            state.getAmount(),
                            state.getNote() != null ? state.getNote() : "Scheduled Transfer",
                            state.getScheduledDateTime(),
                            RecurrenceType.NONE, // Default to one-time transfer
                            null,
                            null);

                    // Initiate transfer
                    try {
                        ApiResponse<ScheduledTransferInitiationResponse> response = scheduledTransferService
                                .initiateScheduledTransfer(request, userId);

                        if (response.isSuccess()) {
                            // Store token for later verification
                            state.setScheduledTransferToken(response.getData().getScheduledTransferToken());
                            state.setOtpSentAt(LocalDateTime.now());
                            state.setStage(ConversationState.TransactionStage.ENTERING_OTP);

                            return Mono.just(
                                    "I've initiated your scheduled transfer. Please check your email " +
                                            "for the verification code and enter it here:");
                        } else {
                            state.reset();
                            state.setStage(ConversationState.TransactionStage.TRANSACTION_FAILED);
                            return Mono.just(
                                    "Sorry, there was an error initiating your transfer: " + response.getMessage());
                        }
                    } catch (Exception e) {
                        log.error("Error initiating scheduled transfer", e);
                        state.reset();
                        state.setStage(ConversationState.TransactionStage.TRANSACTION_FAILED);
                        return Mono.just("Sorry, there was an error: " + e.getMessage());
                    }
                });
    }

    private Mono<String> handleOtpVerification(String userId, String message, ConversationState state) {
        String trimmedMessage = message.trim().toLowerCase();

        // Check for resend OTP request
        if (trimmedMessage.contains("resend") || trimmedMessage.contains("new code") ||
                trimmedMessage.contains("new otp") || trimmedMessage.contains("send again")) {
            return handleOtpResendRequest(userId, state);
        }

        // Increment OTP attempt counter
        state.incrementOtpAttempts();
        String otp = trimmedMessage;

        try {
            // Create verification request
            ScheduledTransferVerificationRequest verificationRequest = new ScheduledTransferVerificationRequest();
            verificationRequest.setOtp(otp);
            verificationRequest.setScheduledTransferToken(state.getScheduledTransferToken());

            // Call verify endpoint
            ApiResponse<ScheduledTransferResponseDTO> response = scheduledTransferService
                    .verifyAndScheduleTransfer(verificationRequest, userId);

            if (response.isSuccess()) {
                state.setStage(ConversationState.TransactionStage.TRANSACTION_COMPLETED);
                state.resetOtpAttempts();

                // Format success message
                ScheduledTransferResponseDTO dto = response.getData();
                String formattedDate = dto.scheduledDateTime().format(DATE_FORMATTER);
                String recipientName = state.getRecipientName();

                if (recipientName == null) {
                    recipientName = "the recipient";
                }

                // Record this transaction for daily limit tracking - only if
                // ScheduledTransferService doesn't do it
                // Note: For scheduled transfers, you might want to record the transaction when
                // it actually happens
                // instead of when it's scheduled, depending on your business rules
                try {
                    accountLimitService.recordTransaction(userId, dto.amount());
                } catch (Exception e) {
                    log.error("Error recording transaction for limit tracking", e);
                    // Don't fail the overall response if just the tracking fails
                }

                return Mono.just(
                        "✅ Success! Your transfer of $" + dto.amount() + " to " +
                                recipientName + " has been scheduled for " + formattedDate + ".");
            } else {
                // OTP verification failed - check if we should allow more attempts
                if (state.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
                    // Too many failed attempts
                    state.setStage(ConversationState.TransactionStage.TRANSACTION_FAILED);
                    return Mono.just("Sorry, you've reached the maximum number of verification attempts. " +
                            "The scheduled transfer has been cancelled. Please try again.");
                } else {
                    // Allow another attempt
                    return Mono.just("The verification code is incorrect. Please try again. " +
                            "You have " + (MAX_OTP_ATTEMPTS - state.getOtpAttempts()) +
                            " attempts remaining.\n\nYou can also type 'resend code' to get a new verification code.");
                }
            }

        } catch (Exception e) {
            log.error("Error verifying scheduled transfer", e);
            // Don't fail immediately - give another chance if not too many attempts
            if (state.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
                state.setStage(ConversationState.TransactionStage.TRANSACTION_FAILED);
                return Mono.just("Sorry, there was an error processing your verification code and you've reached " +
                        "the maximum number of attempts. Please try scheduling your transfer again.");
            } else {
                return Mono.just("Sorry, there was an error verifying your code. Please try again. " +
                        "You can also type 'resend code' if you need a new verification code.");
            }
        }
    }

    private Mono<List<UserBeneficiary>> findBeneficiaries(String userId, String name) {
        if (name == null || name.isEmpty()) {
            return Mono.just(List.of());
        }

        return Mono.fromCallable(() -> {
            // Get the UserBeneficiaries entity with beneficiaries eagerly loaded
            List<UserBeneficiaries> userBeneficiariesList = beneficiaryRepository.findByUserIdWithBeneficiaries(userId);

            // Extract and filter beneficiaries
            return userBeneficiariesList.stream()
                    .flatMap(ub -> ub.getBeneficiaries().stream())
                    .filter(b -> b.getName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());
        });
    }

    private String formatBeneficiarySelectionMessage(List<UserBeneficiary> beneficiaries) {
        StringBuilder message = new StringBuilder(
                "I found multiple people with that name. Who would you like to send to?\n\n");

        for (int i = 0; i < beneficiaries.size(); i++) {
            UserBeneficiary b = beneficiaries.get(i);
            message.append(i + 1).append(". ")
                    .append(b.getName())
                    .append(" (").append(b.getEmail()).append(")");

            if (i < beneficiaries.size() - 1) {
                message.append("\n");
            }
        }

        message.append("\n\nPlease enter the number of your choice.");
        return message.toString();
    }

    private String formatConfirmationMessage(ScheduledTransferDetails details, String recipientName,
            String senderEmail) {
        StringBuilder message = new StringBuilder();
        message.append("I'm about to schedule a transfer of $").append(details.getAmount())
                .append(" to ").append(recipientName)
                .append(" on ").append(details.getScheduledDateTime().format(DATE_FORMATTER));

        if (details.getRecurrenceType() != null && details.getRecurrenceType() != RecurrenceType.NONE) {
            message.append(" (recurring ").append(details.getRecurrenceType().toString().toLowerCase()).append(")");
        }

        if (details.getNote() != null && !details.getNote().isEmpty()) {
            message.append(" with note: \"").append(details.getNote()).append("\"");
        }

        message.append(".\n\nThe money will be sent from your account (").append(senderEmail).append(").")
                .append("\n\nIs this correct? Please reply with 'yes' to confirm or 'no' to cancel.");

        return message.toString();
    }

    private int extractSelectionNumber(String message) {
        message = message.trim();

        try {
            // First try to parse the entire message
            return Integer.parseInt(message);
        } catch (NumberFormatException e) {
            // Then try to extract just the first number
            String[] words = message.split("\s+");
            for (String word : words) {
                try {
                    return Integer.parseInt(word);
                } catch (NumberFormatException ignored) {
                    // Continue to next word
                }
            }

            // If no numbers found, throw exception
            throw new NumberFormatException("No valid selection number found");
        }
    }

    private boolean isPositiveResponse(String message) {
        message = message.toLowerCase().trim();
        return message.contains("yes") || message.contains("correct") || message.contains("confirm") ||
                message.contains("sure") || message.equals("y") || message.contains("proceed");
    }

    private boolean isNegativeResponse(String message) {
        message = message.toLowerCase().trim();
        return message.contains("no") || message.contains("cancel") || message.contains("wrong") ||
                message.contains("incorrect") || message.equals("n");
    }

    /**
     * Handles requests to resend the OTP
     */
    private Mono<String> handleOtpResendRequest(String userId, ConversationState state) {
        // Check if we're in the right stage
        if (state.getStage() != ConversationState.TransactionStage.ENTERING_OTP) {
            return Mono.just("I'm sorry, but there's no active verification process. Please start over.");
        }

        // Check if we're within the cooldown period
        LocalDateTime now = LocalDateTime.now();
        if (state.getOtpSentAt() != null &&
                Duration.between(state.getOtpSentAt(), now).compareTo(OTP_RESEND_COOLDOWN) < 0) {

            // Calculate remaining time
            long secondsRemaining = OTP_RESEND_COOLDOWN.getSeconds() -
                    Duration.between(state.getOtpSentAt(), now).getSeconds();

            return Mono.just("Please wait " + (secondsRemaining / 60) + " minute(s) and " +
                    (secondsRemaining % 60) + " second(s) before requesting a new code.");
        }

        // Get user email for the request
        return Mono.just(keycloakService.getUserById(userId))
                .flatMap(userResponse -> {
                    UserRepresentation user = userResponse.getData();

                    // Recreate the original request
                    ScheduledTransferRequestDTO request = new ScheduledTransferRequestDTO(
                            user.getEmail(),
                            state.getSelectedBeneficiaryEmail(),
                            state.getAmount(),
                            state.getNote() != null ? state.getNote() : "Scheduled Transfer",
                            state.getScheduledDateTime(),
                            RecurrenceType.NONE,
                            null,
                            null);
                    // Request a new OTP
                    try {
                        ApiResponse<ScheduledTransferInitiationResponse> response = scheduledTransferService
                                .initiateScheduledTransfer(request, userId);

                        if (response.isSuccess()) {
                            // Update token and timestamp
                            state.setScheduledTransferToken(response.getData().getScheduledTransferToken());
                            state.setOtpSentAt(LocalDateTime.now());
                            state.resetOtpAttempts();

                            return Mono.just(
                                    "I've sent a new verification code to your email. " +
                                            "Please enter it here when you receive it:");
                        } else {
                            return Mono.just(
                                    "Sorry, I couldn't send a new verification code: " + response.getMessage() +
                                            "\nPlease try entering your existing code again.");
                        }
                    } catch (Exception e) {
                        log.error("Error resending OTP", e);
                        return Mono.just("Sorry, there was an error sending a new verification code. " +
                                "Please try entering your existing code again.");
                    }
                });
    }

    /**
     * Pre-validate transaction limits before proceeding
     * 
     * @param userId the user ID
     * @param amount the transaction amount
     * @return a validation result with success status and message
     */
    private Mono<ValidationResult> preValidateTransactionLimits(String userId, BigDecimal amount) {
        try {
            // Check transfer limit
            if (accountLimitService.wouldExceedTransferLimit(userId, amount)) {
                AccountLimitDTO limits = accountLimitService.getUserLimits(userId);
                return Mono.just(new ValidationResult(false,
                        "I'm sorry, but the transfer amount of $" + amount +
                                " exceeds your transfer limit of $" + limits.getMaxTransferAmount() +
                                ". Would you like to try a different amount?"));
            }

            // Check daily transaction limit
            if (accountLimitService.wouldExceedDailyLimit(userId, amount)) {
                AccountLimitDTO limits = accountLimitService.getUserLimits(userId);
                return Mono.just(new ValidationResult(false,
                        "I'm sorry, but this transfer would exceed your daily transaction limit of $" +
                                limits.getDailyTransactionLimit() + ". You may need to wait until tomorrow or " +
                                "consider upgrading your verification level."));
            }

            return Mono.just(new ValidationResult(true, ""));
        } catch (Exception e) {
            log.error("Error validating transaction limits", e);
            // We'll continue and let the actual transaction service handle the validation
            return Mono.just(new ValidationResult(true, ""));
        }
    }

    /**
     * Check if the recipient's account would exceed balance limits after transfer
     */
    private Mono<ValidationResult> validateRecipientLimits(String recipientEmail, BigDecimal amount) {
        try {
            // Find recipient's userId from their email
            ApiResponse<UserRepresentation> recipientResponse = keycloakService.existsUserByEmail(recipientEmail);

            if (!recipientResponse.isSuccess() || recipientResponse.getData() == null) {
                // Can't find recipient - will let the transfer service handle this
                return Mono.just(new ValidationResult(true, ""));
            }

            String recipientId = recipientResponse.getData().getId();

            // Get recipient's wallet
            try {
                Optional<Wallet> recipientWallet = walletRepository.findWalletByCreatedBy(recipientId);
                if (recipientWallet == null) {
                    // Can't find wallet - will let the transfer service handle this
                    return Mono.just(new ValidationResult(true, ""));
                }

                // Calculate new balance
                BigDecimal newBalance = recipientWallet.get().getBalance().add(amount);

                // Check if this would exceed recipient's balance limit
                if (accountLimitService.wouldExceedBalanceLimit(recipientId, newBalance)) {
                    AccountLimitDTO limits = accountLimitService.getUserLimits(recipientId);
                    return Mono.just(new ValidationResult(false,
                            "I'm sorry, but this transfer would exceed the recipient's wallet balance limit of $" +
                                    limits.getMaxWalletBalance() + ". You might want to try a smaller amount."));
                }
            } catch (Exception e) {
                log.warn("Error checking recipient wallet: {}", e.getMessage());
                // Continue anyway, let the transfer service handle it
                return Mono.just(new ValidationResult(true, ""));
            }

            return Mono.just(new ValidationResult(true, ""));
        } catch (Exception e) {
            log.error("Error validating recipient limits", e);
            // We'll continue and let the actual transaction service handle the validation
            return Mono.just(new ValidationResult(true, ""));
        }
    }

    /**
     * Get the user's current wallet balance.
     * 
     * @param userId User ID
     * @return Current balance
     */
    private BigDecimal getUserBalance(String userId) {
        try {
            Optional<Wallet> wallet = walletRepository.findWalletByCreatedBy(userId);
            if (wallet != null) {
                return wallet.get().getBalance();
            }
        } catch (Exception e) {
            log.error("Error fetching user balance", e);
        }

        // Fallback to a dummy value if service lookup fails
        return new BigDecimal("1000.00");
    }

    // Helper class for validation results
    private static class ValidationResult {
        private final boolean success;
        private final String message;

        public ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
