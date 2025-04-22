package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.enums.RefundStatus;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionStatus;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.kafka.NotificationProducer;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.TransactionReference;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.TransactionReferenceService;
import com.shizzy.moneytransfer.service.TransactionService;
import com.shizzy.moneytransfer.service.WalletService;
import com.shizzy.moneytransfer.service.MoneyTransferService;
import com.shizzy.moneytransfer.service.OtpService;

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
                referenceNumber
        );

        debitTransaction.setCurrentStatus(TransactionStatus.PENDING.getValue());
        transactionRepository.save(debitTransaction);

        final Transaction creditTransaction = transactionService.createTransaction(
                receivingWallet,
                requestBody,
                TransactionType.CREDIT,
                TransactionOperation.TRANSFER,
                creditDescription,
                referenceNumber
        );

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
                referenceNumber
        );

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

    private void updateRefundableAmount(Wallet sendingWallet, BigDecimal remainingAmount) {
        List<Transaction> senderDeposits = transactionRepository
                .findByWalletIdAndOperationAndRefundStatusNot(sendingWallet.getId(),
                        TransactionOperation.DEPOSIT, RefundStatus.NON_REFUNDABLE);

        for (Transaction deposit : senderDeposits) {
            BigDecimal refundableAmount = deposit.getRefundableAmount();
            if (remainingAmount.compareTo(refundableAmount) >= 0) {
                remainingAmount = remainingAmount.subtract(refundableAmount);
                deposit.setRefundableAmount(BigDecimal.ZERO);
                deposit.setRefundStatus(RefundStatus.NON_REFUNDABLE);
            } else {
                deposit.setRefundableAmount(refundableAmount.subtract(remainingAmount));
                deposit.setRefundStatus(RefundStatus.PARTIALLY_REFUNDABLE);
                remainingAmount = BigDecimal.ZERO;
            }
            transactionRepository.save(deposit);

            if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
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
            operationDetails
        );
        
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
            request.getOtp()
        );
        
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
}