package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.enums.RefundImpactType;
import com.shizzy.moneytransfer.enums.RefundStatus;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionStatus;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.exception.TransactionLimitExceededException;
import com.shizzy.moneytransfer.kafka.NotificationProducer;
import com.shizzy.moneytransfer.model.RefundImpactRecord;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.TransactionReference;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.RefundImpactRecordRepository;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.AccountLimitService;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.TransactionReferenceService;
import com.shizzy.moneytransfer.service.TransactionService;
import com.shizzy.moneytransfer.service.WalletService;
import com.shizzy.moneytransfer.service.MoneyTransferService;
import com.shizzy.moneytransfer.service.OtpService;
import com.shizzy.moneytransfer.service.TransactionLimitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Service
@Slf4j
public class MoneyTransferServiceImpl implements MoneyTransferService {
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final TransactionReferenceService referenceService;
    private final KeycloakService keycloakService;
    private final NotificationProducer notificationProducer;
    private final TransactionService transactionService;
    private final OtpService otpService;
    private final TransactionLimitService transactionLimitService;
    private final AccountLimitService accountLimitService;
    private final RefundImpactRecordRepository refundImpactRecordRepository;

    // In-memory store for pending transfers
    private final Map<String, PendingTransfer> pendingTransfers = new ConcurrentHashMap<>();

    // Constants
    private static final String TRANSFER_OPERATION = "Money Transfer";
    private static final Duration TRANSFER_EXPIRY = Duration.ofMinutes(15);

    // Inner class to track pending transfers with timestamps
    private static class PendingTransfer {
        private final CreateTransactionRequestBody requestBody;
        private final LocalDateTime createdAt;

        public PendingTransfer(CreateTransactionRequestBody requestBody) {
            this.requestBody = requestBody;
            this.createdAt = LocalDateTime.now();
        }

        public CreateTransactionRequestBody getRequestBody() {
            return requestBody;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(createdAt.plus(TRANSFER_EXPIRY));
        }
    }

    @Override
    public ApiResponse<TransactionResponseDTO> transfer(CreateTransactionRequestBody requestBody) {
        // Validate request and participants
        validateSenderReceiver(requestBody.senderEmail(), requestBody.receiverEmail());
        TransferInfo transferInfo = fetchSenderAndReceiverId(
                requestBody.senderEmail(), requestBody.receiverEmail());

        // Retrieve wallets and validate balance
        Wallet sendingWallet = walletService.findWalletOrThrow(transferInfo.getSenderId());
        Wallet receivingWallet = walletService.findWalletOrThrow(transferInfo.getReceiverId());
        walletService.verifyWalletBalance(sendingWallet.getBalance(), requestBody.amount());

        // Validate against transaction limits
        transactionLimitService.validateTransfer(transferInfo.getSenderId(), requestBody.amount());

        // Generate reference and process transaction
        String referenceNumber = referenceService.generateUniqueReferenceNumber();
        TransactionPair transactions = createTransactionPair(
                sendingWallet, receivingWallet, requestBody, transferInfo, referenceNumber);

        // Process transfer and update status
        walletService.transfer(sendingWallet, receivingWallet, requestBody.amount());
        saveTransactionReference(transactions, referenceNumber);
        updateTransactionStatuses(transactions, TransactionStatus.SUCCESS);

        // Post-processing
        updateRefundableAmount(sendingWallet, requestBody.amount());
        sendTransferNotification(transferInfo, transactions);

        // Record the transaction for daily limit tracking
        accountLimitService.recordTransaction(transferInfo.getSenderId(), requestBody.amount());

        return buildSuccessResponse(transactions, sendingWallet, receivingWallet, referenceNumber);
    }

    private TransactionPair createTransactionPair(
            Wallet sendingWallet,
            Wallet receivingWallet,
            CreateTransactionRequestBody requestBody,
            TransferInfo transferInfo,
            String referenceNumber) {

        String debitDescription = "Wallet to wallet Transfer to " + transferInfo.getReceiverName();
        String creditDescription = "Wallet to wallet Transfer from " + transferInfo.getSenderName();

        final Transaction debitTransaction = transactionService.createTransaction(
                sendingWallet,
                requestBody,
                TransactionType.DEBIT,
                TransactionOperation.TRANSFER,
                debitDescription,
                referenceNumber);

        debitTransaction.setCurrentStatus(TransactionStatus.PENDING.getValue());
        transactionRepository.save(debitTransaction);

        final Transaction creditTransaction = transactionService.createTransaction(
                receivingWallet,
                requestBody,
                TransactionType.CREDIT,
                TransactionOperation.TRANSFER,
                creditDescription,
                referenceNumber);

        creditTransaction.setCurrentStatus(TransactionStatus.PENDING.getValue());
        transactionRepository.save(creditTransaction);

        return new TransactionPair(debitTransaction, creditTransaction);
    }

    private void saveTransactionReference(TransactionPair transactions, String referenceNumber) {
        TransactionReference transactionReference = TransactionReference.builder()
                .referenceNumber(referenceNumber)
                .debitTransaction(transactions.getDebitTransaction())
                .creditTransaction(transactions.getCreditTransaction())
                .build();

        referenceService.saveTransactionReference(transactionReference, "");
    }

    private void updateTransactionStatuses(TransactionPair transactions, TransactionStatus status) {
        transactions.getCreditTransaction().setCurrentStatus(status.getValue());
        transactions.getDebitTransaction().setCurrentStatus(status.getValue());

        transactionRepository.save(transactions.getCreditTransaction());
        transactionRepository.save(transactions.getDebitTransaction());
    }

    private void sendTransferNotification(TransferInfo transferInfo, TransactionPair transactions) {
        TransactionNotification notification = TransactionNotification.builder()
                .operation(TransactionOperation.TRANSFER)
                .transferInfo(transferInfo)
                .debitTransaction(transactions.getDebitTransaction())
                .creditTransaction(transactions.getCreditTransaction())
                .build();

        notificationProducer.sendNotification("notifications", notification);
    }

    private ApiResponse<TransactionResponseDTO> buildSuccessResponse(
            TransactionPair transactions,
            Wallet sendingWallet,
            Wallet receivingWallet,
            String referenceNumber) {

        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE MMMM dd uuuu", Locale.getDefault());
        Transaction debitTransaction = transactions.getDebitTransaction();

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                dtf.format(debitTransaction.getTransactionDate()),
                debitTransaction.getAmount(),
                debitTransaction.getCurrentStatus(),
                debitTransaction.getTransactionType(),
                sendingWallet.getCreatedBy(),
                sendingWallet.getWalletId(),
                receivingWallet.getCreatedBy(),
                receivingWallet.getWalletId(),
                referenceNumber);

        return ApiResponse.<TransactionResponseDTO>builder()
                .success(true)
                .message("Transfer successful")
                .data(responseDTO)
                .build();
    }

    private void validateSenderReceiver(String senderEmail, String receiverEmail) {
        if (senderEmail.equals(receiverEmail)) {
            throw new IllegalArgumentException("You cannot send money to yourself.");
        }
    }

    private TransferInfo fetchSenderAndReceiverId(String senderEmail, String receiverEmail) {
        UserRepresentation sender = keycloakService.existsUserByEmail(senderEmail).getData();
        UserRepresentation receiver = keycloakService.existsUserByEmail(receiverEmail).getData();

        return TransferInfo.builder()
                .senderId(sender.getId())
                .receiverId(receiver.getId())
                .senderName(sender.getFirstName() + " " + sender.getLastName())
                .receiverName(receiver.getFirstName() + " " + receiver.getLastName())
                .senderEmail(sender.getEmail())
                .receiverEmail(receiver.getEmail())
                .build();
    }

    private void updateRefundableAmount(Wallet sendingWallet, BigDecimal transferAmount) {
        log.info("Updating refundable amounts for wallet {} after transfer of {}",
                sendingWallet.getWalletId(), transferAmount);

        BigDecimal remainingToDeduct = transferAmount;

        // Get all refundable deposits ordered by date (oldest first) - FIFO approach
        List<Transaction> senderDeposits = transactionRepository
                .findByWalletIdAndOperationAndRefundStatusNotOrderByTransactionDateAsc(
                        sendingWallet.getId(),
                        TransactionOperation.DEPOSIT,
                        RefundStatus.NON_REFUNDABLE);

        if (senderDeposits.isEmpty()) {
            log.info("No refundable deposits found. Transfer likely used previously received funds.");
            return;
        }

        log.debug("Found {} refundable deposits to process", senderDeposits.size());

        for (Transaction deposit : senderDeposits) {
            BigDecimal depositRefundableAmount = deposit.getRefundableAmount();
            log.debug("Processing deposit ID: {}, reference: {}, refundable amount: {}",
                    deposit.getTransactionId(), deposit.getReferenceNumber(), depositRefundableAmount);

            if (depositRefundableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // Skip deposits with no refundable amount
            }

            // Calculate how much to deduct from this deposit
            BigDecimal amountToDeduct = remainingToDeduct.min(depositRefundableAmount);
            BigDecimal newRefundableAmount = depositRefundableAmount.subtract(amountToDeduct);

            // Update deposit's refundable amount
            deposit.setRefundableAmount(newRefundableAmount);

            // Update refund status based on new refundable amount
            if (newRefundableAmount.compareTo(BigDecimal.ZERO) == 0) {
                deposit.setRefundStatus(RefundStatus.NON_REFUNDABLE);
                log.debug("Deposit ID: {} is now NON_REFUNDABLE", deposit.getTransactionId());
            } else if (newRefundableAmount.compareTo(deposit.getAmount()) < 0) {
                deposit.setRefundStatus(RefundStatus.PARTIALLY_REFUNDABLE);
                log.debug("Deposit ID: {} is now PARTIALLY_REFUNDABLE with amount {}",
                        deposit.getTransactionId(), newRefundableAmount);
            }
            // Create a refund impact record for audit trail
            createRefundImpactRecord(deposit, amountToDeduct, transferAmount);

            // Save updated deposit
            transactionRepository.save(deposit);

            // Update remaining amount to deduct
            remainingToDeduct = remainingToDeduct.subtract(amountToDeduct);

            if (remainingToDeduct.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

        // If we've gone through all deposits and still have amount to deduct,
        // the transfer used previously received funds (not refundable deposits)
        if (remainingToDeduct.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Transfer partially used non-deposit funds: {}", remainingToDeduct);
        }
    }

    @Override
    public ApiResponse<TransferInitiationResponse> initiateTransfer(CreateTransactionRequestBody requestBody,
            String userId) {
        // Validate request and user
        validateSenderReceiver(requestBody.senderEmail(), requestBody.receiverEmail());

        // Verify it's the user's own email
        UserRepresentation user = keycloakService.getUserById(userId).getData();
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (!user.getEmail().equals(requestBody.senderEmail())) {
            throw new IllegalArgumentException("You can only transfer from your own account");
        }

        // Pre-validate the transfer against account limits
        try {
            // Get sender and receiver information
            TransferInfo transferInfo = fetchSenderAndReceiverId(
                    requestBody.senderEmail(), requestBody.receiverEmail());

            // Check if sender has sufficient balance
            Wallet sendingWallet = walletService.findWalletOrThrow(transferInfo.getSenderId());
            walletService.verifyWalletBalance(sendingWallet.getBalance(), requestBody.amount());

            // Validate against transfer limits
            transactionLimitService.validateTransfer(userId, requestBody.amount());

            // Validate against daily transaction limits
            if (accountLimitService.wouldExceedDailyLimit(userId, requestBody.amount())) {
                throw new TransactionLimitExceededException(
                        "This transfer would exceed your daily transaction limit");
            }

            // Check if recipient wallet would exceed balance limit
            Wallet receivingWallet = walletService.findWalletOrThrow(transferInfo.getReceiverId());
            BigDecimal newReceiverBalance = receivingWallet.getBalance().add(requestBody.amount());
            transactionLimitService.validateNewBalance(transferInfo.getReceiverId(), newReceiverBalance);

        } catch (TransactionLimitExceededException e) {
            // Return friendly error for limit exceeded
            return ApiResponse.<TransferInitiationResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            // Log other errors but continue with OTP process for non-critical validations
            log.warn("Pre-validation warning (will continue with OTP): {}", e.getMessage());
        }

        // Generate unique token for this transfer request
        String transferToken = UUID.randomUUID().toString();

        // Store pending transfer in our in-memory map
        pendingTransfers.put(transferToken, new PendingTransfer(requestBody));
        log.info("Stored pending transfer with token: {}", transferToken);

        // Get recipient name for OTP email
        TransferInfo transferInfo = fetchSenderAndReceiverId(
                requestBody.senderEmail(), requestBody.receiverEmail());

        // Prepare details for OTP email
        Map<String, Object> operationDetails = new HashMap<>();
        operationDetails.put("amount", requestBody.amount().toString());
        operationDetails.put("recipient", transferInfo.getReceiverName());
        operationDetails.put("recipient_email", requestBody.receiverEmail());

        // Send OTP
        otpService.sendOtp(
                user.getEmail(),
                user.getFirstName(),
                TRANSFER_OPERATION,
                operationDetails);

        return ApiResponse.<TransferInitiationResponse>builder()
                .success(true)
                .message("Transfer initiated. Please check your email for verification code.")
                .data(new TransferInitiationResponse(transferToken))
                .build();
    }

    @Override
    public ApiResponse<TransactionResponseDTO> verifyAndTransfer(TransferVerificationRequest request, String userId) {
        // Verify user
        ApiResponse<UserRepresentation> userResponse = keycloakService.getUserById(userId);
        UserRepresentation user = userResponse.getData();
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        log.info("Verifying transfer request with token: {}", request.getTransferToken());

        // Verify OTP
        Map<String, Object> operationDetails = otpService.verifyOtp(
                user.getEmail(),
                TRANSFER_OPERATION,
                request.getOtp());

        if (operationDetails == null) {
            log.warn("Invalid or expired OTP for user: {}", user.getEmail());
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        // Retrieve pending transfer from our in-memory map
        PendingTransfer pendingTransfer = pendingTransfers.get(request.getTransferToken());

        if (pendingTransfer == null) {
            log.warn("Transfer request not found for token: {}", request.getTransferToken());
            throw new IllegalArgumentException("Transfer request expired or not found");
        }

        // Check if transfer request is expired
        if (pendingTransfer.isExpired()) {
            log.warn("Transfer request expired for token: {}", request.getTransferToken());
            pendingTransfers.remove(request.getTransferToken());
            throw new IllegalArgumentException("Transfer request expired");
        }

        CreateTransactionRequestBody requestBody = pendingTransfer.getRequestBody();

        // Clean up
        pendingTransfers.remove(request.getTransferToken());
        log.info("Removed pending transfer with token: {}", request.getTransferToken());

        // Execute the actual transfer
        return transfer(requestBody);
    }

    /**
     * Scheduled task to clean up expired pending transfers
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5 minutes
    public void cleanupExpiredTransfers() {
        int removedCount = 0;

        for (Map.Entry<String, PendingTransfer> entry : pendingTransfers.entrySet()) {
            if (entry.getValue().isExpired()) {
                pendingTransfers.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} expired pending transfers", removedCount);
        }
    }

    /**
     * Creates an audit record of how a transfer impacted a deposit's refundable
     * amount
     */
    private void createRefundImpactRecord(Transaction deposit, BigDecimal amountDeducted,
            BigDecimal totalTransferAmount) {
        RefundImpactRecord impactRecord = RefundImpactRecord.builder()
                .depositTransactionId(deposit.getTransactionId())
                .impactAmount(amountDeducted.negate()) // Negative because we're reducing refundable amount
                .impactType(RefundImpactType.TRANSFER)
                .impactDate(LocalDateTime.now())
                .previousRefundableAmount(deposit.getRefundableAmount().add(amountDeducted))
                .newRefundableAmount(deposit.getRefundableAmount())
                .relatedTransferAmount(totalTransferAmount)
                .build();

        refundImpactRecordRepository.save(impactRecord);
    }
}