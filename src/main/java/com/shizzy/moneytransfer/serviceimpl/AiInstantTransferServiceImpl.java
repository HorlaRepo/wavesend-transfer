package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.client.GeminiAiClient;
import com.shizzy.moneytransfer.dto.*;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInstantTransferServiceImpl {

    private final UserBeneficiariesRepository beneficiaryRepository;
    private final AiEntityExtractionService aiEntityExtractionService;
    private final KeycloakService keycloakService;
    private final MoneyTransferService moneyTransferService;
    private final AccountLimitService accountLimitService;
    private final TransactionLimitService transactionLimitService;
    private final WalletRepository walletRepository;

    // Constants
    private static final Duration OTP_RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    /**
     * Handle an instant transfer request from the user. This is the main entry
     * point
     * for the instant transfer flow.
     * 
     * @param userId  User ID
     * @param message User message
     * @param state   Conversation state
     * @return Response to user
     */
    public Mono<String> handleInstantTransferRequest(String userId, String message, ConversationState state) {
        // Handle based on current state
        return switch (state.getStage()) {
            case NONE -> handleInitialRequest(userId, message, state);
            case SELECTING_BENEFICIARY -> handleBeneficiarySelection(userId, message, state);
            case ENTERING_RECIPIENT_EMAIL -> handleRecipientEmailInput(userId, message, state);
            case CONFIRMING_TRANSACTION -> handleConfirmation(userId, message, state);
            case ENTERING_OTP -> handleOtpVerification(userId, message, state);
            case TRANSACTION_COMPLETED, TRANSACTION_FAILED ->
                Mono.just("Type a new message to start another transaction.");
        };
    }

    /**
     * Handle the initial request for an instant transfer.
     * 
     * @param userId  User ID
     * @param message User message
     * @param state   Conversation state
     * @return Response to user
     */

    private Mono<String> handleInitialRequest(String userId, String message, ConversationState state) {
        // Extract transfer details from message
        return aiEntityExtractionService.extractTransferDetails(userId, message)
                .flatMap(details -> {
                    // Set the extracted details in the state
                    state.setAmount(details.getAmount());
                    state.setRecipientName(details.getRecipientName());
                    state.setNote(details.getNote());

                    // Pre-validate transaction limits (before displaying options to user)
                    return preValidateTransactionLimits(userId, details.getAmount())
                            .flatMap(limitValidationResult -> {
                                // If validation failed, return error message immediately
                                if (!limitValidationResult.isSuccess()) {
                                    return Mono.just(limitValidationResult.getMessage());
                                }

                                // Continue with regular flow...
                                if (isValidEmail(details.getRecipientName())) {
                                    // Direct email transfer
                                    return handleDirectEmailTransfer(userId, details.getRecipientName(), state);
                                }

                                // Check for insufficient balance here
                                return Mono.just(keycloakService.getUserById(userId))
                                        .flatMap(userResponse -> {
                                            UserRepresentation user = userResponse.getData();

                                            // Fetch user's wallet balance
                                            try {
                                                BigDecimal balance = getUserBalance(userId);

                                                // Check if user has enough balance
                                                if (balance.compareTo(details.getAmount()) < 0) {
                                                    return Mono.just(
                                                            "I'm sorry, but you don't have enough balance for this transfer. "
                                                                    +
                                                                    "Your current balance is $" + balance
                                                                    + " but you're trying to send $" +
                                                                    details.getAmount()
                                                                    + ". Would you like to try a different amount?");
                                                }
                                            } catch (Exception e) {
                                                log.error("Error checking user balance", e);
                                                // Continue with flow - we'll check balance again during actual transfer
                                            }
                                            // Find beneficiaries matching the recipient name
                                            return findBeneficiaries(userId, details.getRecipientName())
                                                    .flatMap(beneficiaries -> {
                                                        state.setMatchingBeneficiaries(beneficiaries);

                                                        if (beneficiaries.isEmpty()) {
                                                            // No matching beneficiaries found - ask for email
                                                            state.setStage(
                                                                    ConversationState.TransactionStage.ENTERING_RECIPIENT_EMAIL);
                                                            return Mono.just(
                                                                    "I couldn't find any beneficiaries matching '" +
                                                                            details.getRecipientName() + "'. " +
                                                                            "Please enter the recipient's email address instead:");
                                                        } else if (beneficiaries.size() == 1) {
                                                            // Exactly one match - select automatically
                                                            UserBeneficiary beneficiary = beneficiaries.get(0);
                                                            state.setSelectedBeneficiaryEmail(beneficiary.getEmail());
                                                            state.setStage(
                                                                    ConversationState.TransactionStage.CONFIRMING_TRANSACTION);

                                                            return Mono.just(formatConfirmationMessage(
                                                                    details,
                                                                    beneficiary.getName(),
                                                                    user.getEmail()));
                                                        } else {
                                                            // Multiple matches - ask user to select
                                                            state.setStage(
                                                                    ConversationState.TransactionStage.SELECTING_BENEFICIARY);

                                                            StringBuilder response = new StringBuilder(
                                                                    "I found multiple beneficiaries matching that name. Please select one:\n\n");

                                                            for (int i = 0; i < beneficiaries.size(); i++) {
                                                                response.append(i + 1).append(". ")
                                                                        .append(beneficiaries.get(i).getName())
                                                                        .append(" (")
                                                                        .append(beneficiaries.get(i).getEmail())
                                                                        .append(")\n");
                                                            }

                                                            response.append(
                                                                    "\nYou can also type a different name or enter an email address directly.");

                                                            return Mono.just(response.toString());
                                                        }
                                                    });
                                        });
                            });
                });
    }

    /**
     * Handle direct transfer to an email address.
     * 
     * @param userId         User ID
     * @param recipientEmail Recipient email address
     * @param state          Conversation state
     * @return Response to user
     */
    private Mono<String> handleDirectEmailTransfer(String userId, String recipientEmail, ConversationState state) {
        // Verify it's a valid email
        if (!isValidEmail(recipientEmail)) {
            state.setStage(ConversationState.TransactionStage.ENTERING_RECIPIENT_EMAIL);
            return Mono.just("That doesn't look like a valid email address. Please enter a valid email:");
        }

        // Validate recipient limits
        return validateRecipientLimits(recipientEmail, state.getAmount())
                .flatMap(limitValidationResult -> {
                    // If validation failed, return error message and reset state
                    if (!limitValidationResult.isSuccess()) {
                        state.reset();
                        return Mono.just(limitValidationResult.getMessage());
                    }

                    // Set the recipient email and move to confirmation
                    state.setSelectedBeneficiaryEmail(recipientEmail);
                    state.setStage(ConversationState.TransactionStage.CONFIRMING_TRANSACTION);

                    return Mono.just(keycloakService.getUserById(userId))
                            .map(userResponse -> {
                                UserRepresentation user = userResponse.getData();

                                return formatConfirmationMessage(
                                        new TransferDetails(
                                                state.getAmount(),
                                                recipientEmail,
                                                state.getNote()),
                                        recipientEmail, // Use email as name
                                        user.getEmail());
                            });
                });
    }

    /**
     * Handle recipient email input.
     * 
     * @param userId  User ID
     * @param message User message containing an email
     * @param state   Conversation state
     * @return Response to user
     */
    private Mono<String> handleRecipientEmailInput(String userId, String message, ConversationState state) {
        String email = message.trim();

        // Check if it's a valid email
        if (isValidEmail(email)) {
            return handleDirectEmailTransfer(userId, email, state);
        } else {
            return Mono.just(
                    "That doesn't look like a valid email address. Please enter a valid email, or type 'cancel' to start over:");
        }
    }

    /**
     * Handle beneficiary selection from the list of options.
     * 
     * @param userId  User ID
     * @param message User message
     * @param state   Conversation state
     * @return Response to user
     */
    @Transactional(readOnly = true)
    private Mono<String> handleBeneficiarySelection(String userId, String message, ConversationState state) {
        String trimmedMessage = message.trim();

        // Check if user entered an email instead
        if (isValidEmail(trimmedMessage)) {
            return handleDirectEmailTransfer(userId, trimmedMessage, state);
        }

        // Try to parse user selection as a number
        try {
            int selection = Integer.parseInt(trimmedMessage);
            List<UserBeneficiary> matches = state.getMatchingBeneficiaries();

            if (selection > 0 && selection <= matches.size()) {
                // Valid selection
                UserBeneficiary selected = matches.get(selection - 1);
                state.setSelectedBeneficiaryEmail(selected.getEmail());
                state.setStage(ConversationState.TransactionStage.CONFIRMING_TRANSACTION);

                return Mono.just(keycloakService.getUserById(userId))
                        .map(userResponse -> {
                            UserRepresentation user = userResponse.getData();

                            return formatConfirmationMessage(
                                    new TransferDetails(
                                            state.getAmount(),
                                            selected.getName(),
                                            state.getNote()),
                                    selected.getName(),
                                    user.getEmail());
                        });
            } else {
                return Mono.just(
                        "Please enter a valid number from the list, provide an email address, or type 'cancel' to start over.");
            }
        } catch (NumberFormatException e) {
            List<UserBeneficiary> matches = state.getMatchingBeneficiaries();

            // Check if message contains a name that uniquely identifies a beneficiary
            List<UserBeneficiary> filtered = matches.stream()
                    .filter(b -> b.getName().toLowerCase().contains(trimmedMessage.toLowerCase()))
                    .collect(Collectors.toList());

            if (filtered.size() == 1) {
                // Unique match found by name
                UserBeneficiary selected = filtered.get(0);
                state.setSelectedBeneficiaryEmail(selected.getEmail());
                state.setStage(ConversationState.TransactionStage.CONFIRMING_TRANSACTION);

                return Mono.just(keycloakService.getUserById(userId))
                        .map(userResponse -> {
                            UserRepresentation user = userResponse.getData();

                            return formatConfirmationMessage(
                                    new TransferDetails(
                                            state.getAmount(),
                                            selected.getName(),
                                            state.getNote()),
                                    selected.getName(),
                                    user.getEmail());
                        });
            } else {
                return Mono
                        .just("I'm not sure which person you meant. Please enter just the number next to their name, "
                                + "or provide an email address directly.");
            }
        }
    }

    /**
     * Handle user confirmation of the transfer.
     * 
     * @param userId  User ID
     * @param message User message
     * @param state   Conversation state
     * @return Response to user
     */
    private Mono<String> handleConfirmation(String userId, String message, ConversationState state) {
        // Check if user confirmed
        if (isPositiveResponse(message)) {
            // Initiate transfer
            return initiateTransfer(userId, state);
        } else if (isNegativeResponse(message)) {
            // User declined, reset
            state.reset();
            return Mono.just("I've cancelled the transfer. Is there anything else you'd like to do?");
        } else {
            // Unclear response
            return Mono.just(
                    "Please confirm if you want to proceed with this transfer. Say yes to proceed or no to cancel.");
        }
    }

    /**
     * Handle OTP verification for the transfer.
     * 
     * @param userId  User ID
     * @param message User message
     * @param state   Conversation state
     * @return Response to user
     */
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
            TransferVerificationRequest verificationRequest = new TransferVerificationRequest();
            verificationRequest.setOtp(otp);
            verificationRequest.setTransferToken(state.getTransferToken());

            // Call verify endpoint
            ApiResponse<TransactionResponseDTO> response = moneyTransferService
                    .verifyAndTransfer(verificationRequest, userId);

            if (response.isSuccess()) {
                state.setStage(ConversationState.TransactionStage.TRANSACTION_COMPLETED);
                state.resetOtpAttempts();

                // Format success message
                TransactionResponseDTO dto = response.getData();
                String recipientName = state.getRecipientName();

                // If recipient name is an email or null, use the email directly
                if (recipientName == null || isValidEmail(recipientName)) {
                    recipientName = state.getSelectedBeneficiaryEmail();
                }

                return Mono.just(
                        "✅ Success! Your transfer of $" + dto.amount() + " to " +
                                recipientName + " has been completed.\n\n" +
                                "Reference Number: " + dto.referenceNumber());
            } else {
                // OTP verification failed - check if we should allow more attempts
                if (state.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
                    // Too many failed attempts
                    state.setStage(ConversationState.TransactionStage.TRANSACTION_FAILED);
                    return Mono.just("Sorry, you've reached the maximum number of verification attempts. " +
                            "The transfer has been cancelled. Please try again.");
                } else {
                    // Allow another attempt
                    return Mono.just("The verification code is incorrect. Please try again. " +
                            "You have " + (MAX_OTP_ATTEMPTS - state.getOtpAttempts()) +
                            " attempts remaining.\n\nYou can also type 'resend code' to get a new verification code.");
                }
            }
        } catch (Exception e) {
            log.error("Error verifying transfer", e);

            // Don't fail immediately - give another chance if not too many attempts
            if (state.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
                state.setStage(ConversationState.TransactionStage.TRANSACTION_FAILED);
                return Mono.just("Sorry, there was an error processing your verification code and you've reached " +
                        "the maximum number of attempts. Please try your transfer again.");
            } else {
                return Mono.just("Sorry, there was an error verifying your code. Please try again. " +
                        "You can also type 'resend code' if you need a new verification code.");
            }
        }
    }

    /**
     * Handle a request to resend the OTP.
     * 
     * @param userId User ID
     * @param state  Conversation state
     * @return Response to user
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
                    CreateTransactionRequestBody request = new CreateTransactionRequestBody(
                            user.getEmail(),
                            state.getSelectedBeneficiaryEmail(),
                            state.getAmount(),
                            state.getNote() != null ? state.getNote() : "Instant Transfer");

                    // Request a new OTP
                    try {
                        ApiResponse<TransferInitiationResponse> response = moneyTransferService
                                .initiateTransfer(request, userId);

                        if (response.isSuccess()) {
                            // Update token and timestamp
                            state.setTransferToken(response.getData().getTransferToken());
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
     * Initiate the transfer with the Money Transfer Service.
     * 
     * @param userId User ID
     * @param state  Conversation state
     * @return Response to user
     */
    private Mono<String> initiateTransfer(String userId, ConversationState state) {
        // Get user email
        return Mono.just(keycloakService.getUserById(userId))
                .flatMap(userResponse -> {
                    UserRepresentation user = userResponse.getData();

                    // Final validation of transaction limits
                    try {
                        // Validate transfer limit
                        transactionLimitService.validateTransfer(userId, state.getAmount());

                        // Validate recipient limits if possible
                        ValidationResult recipientValidation = validateRecipientLimits(
                                state.getSelectedBeneficiaryEmail(), state.getAmount()).block();

                        if (recipientValidation != null && !recipientValidation.isSuccess()) {
                            state.reset();
                            return Mono.just(recipientValidation.getMessage());
                        }
                    } catch (TransactionLimitExceededException e) {
                        state.reset();
                        return Mono.just("I'm sorry, but " + e.getMessage() +
                                ". Would you like to try a different amount?");
                    } catch (Exception e) {
                        // Log but continue - let the service handle it
                        log.warn("Error during final validation (will attempt anyway): {}", e.getMessage());
                    }

                    // Create request
                    CreateTransactionRequestBody request = new CreateTransactionRequestBody(
                            user.getEmail(),
                            state.getSelectedBeneficiaryEmail(),
                            state.getAmount(),
                            state.getNote() != null ? state.getNote() : "Instant Transfer");

                    // Initiate transfer
                    try {
                        ApiResponse<TransferInitiationResponse> response = moneyTransferService
                                .initiateTransfer(request, userId);

                        if (response.isSuccess()) {
                            // Store token for later verification and record time
                            state.setTransferToken(response.getData().getTransferToken());
                            state.setOtpSentAt(LocalDateTime.now());
                            state.setStage(ConversationState.TransactionStage.ENTERING_OTP);

                            return Mono.just(
                                    "I've initiated your transfer. Please check your email " +
                                            "for the verification code and enter it here:");
                        } else {
                            state.reset();
                            state.setStage(ConversationState.TransactionStage.TRANSACTION_FAILED);
                            return Mono.just(
                                    "Sorry, there was an error initiating your transfer: " + response.getMessage());
                        }
                    } catch (Exception e) {
                        log.error("Error initiating transfer", e);
                        state.reset();
                        state.setStage(ConversationState.TransactionStage.TRANSACTION_FAILED);

                        // Check for specific error messages
                        String errorMessage = e.getMessage();
                        if (errorMessage != null) {
                            if (errorMessage.contains("balance")) {
                                return Mono.just(
                                        "Sorry, there was an error: Insufficient balance to complete this transfer.");
                            } else if (errorMessage.contains("limit") || errorMessage.contains("exceed")) {
                                return Mono.just("Sorry, there was an error: " + errorMessage +
                                        ". You may need to upgrade your verification level for higher limits.");
                            }
                        }
                        return Mono.just("Sorry, there was an error: " + errorMessage);
                    }
                });
    }

    /**
     * Format a confirmation message for the transfer.
     * 
     * @param details       Transfer details
     * @param recipientName Recipient name
     * @param senderEmail   Sender email
     * @return Formatted confirmation message
     */
    private String formatConfirmationMessage(TransferDetails details, String recipientName, String senderEmail) {
        StringBuilder message = new StringBuilder();
        message.append("I'm about to transfer $").append(String.format("%.2f", details.getAmount()))
                .append(" to ").append(recipientName).append(".\n\n");

        message.append("The money will be sent from your account (").append(senderEmail).append(").");

        if (details.getNote() != null && !details.getNote().isEmpty()) {
            message.append("\n\nDescription: ").append(details.getNote());
        }

        message.append("\n\nIs this correct? Please reply with 'yes' to confirm or 'no' to cancel.");
        return message.toString();
    }

    /**
     * Find beneficiaries matching a name.
     * 
     * @param userId User ID
     * @param name   Name to match
     * @return List of matching beneficiaries
     */
    @Transactional(readOnly = true)
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

    /**
     * Check if the input is a valid email address.
     * 
     * @param input String to check
     * @return True if valid email
     */
    private boolean isValidEmail(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(input).matches();
    }

    /**
     * Get the user's current wallet balance.
     * 
     * @param userId User ID
     * @return Current balance
     */
    private BigDecimal getUserBalance(String userId) {
        // This would be replaced with a call to your wallet service
        // For now, we'll just return a dummy value
        return new BigDecimal("1000.00");
    }

    /**
     * Check if a message is a positive response.
     * 
     * @param message Message to check
     * @return True if positive
     */
    private boolean isPositiveResponse(String message) {
        String lowerCase = message.toLowerCase().trim();
        return lowerCase.equals("yes") || lowerCase.equals("y") ||
                lowerCase.equals("yep") || lowerCase.equals("yeah") ||
                lowerCase.equals("sure") || lowerCase.equals("ok") ||
                lowerCase.equals("confirm") || lowerCase.equals("proceed");
    }

    /**
     * Check if a message is a negative response.
     * 
     * @param message Message to check
     * @return True if negative
     */
    private boolean isNegativeResponse(String message) {
        String lowerCase = message.toLowerCase().trim();
        return lowerCase.equals("no") || lowerCase.equals("n") ||
                lowerCase.equals("nope") || lowerCase.equals("cancel") ||
                lowerCase.equals("stop") || lowerCase.equals("don't") ||
                lowerCase.equals("dont");
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

            // Also check if the transaction would exceed recipient balance limits
            // This is more complex since we don't know the recipient yet, so will do this
            // later

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