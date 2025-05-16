package com.shizzy.moneytransfer.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.enums.RefundImpactType;
import com.shizzy.moneytransfer.enums.RefundStatus;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.exception.PaymentException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.RefundImpactRecord;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.TransactionReference;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.RefundImpactRecordRepository;
import com.shizzy.moneytransfer.repository.TransactionReferenceRepository;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.*;
import com.shizzy.moneytransfer.service.payment.StripePaymentProcessor;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.param.RefundCreateParams;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.shizzy.moneytransfer.enums.RefundStatus.*;
import static com.shizzy.moneytransfer.enums.TransactionOperation.*;
import static com.shizzy.moneytransfer.enums.TransactionSource.*;
import static com.shizzy.moneytransfer.enums.TransactionStatus.*;
import static com.shizzy.moneytransfer.enums.TransactionType.*;
import static com.shizzy.moneytransfer.util.CacheNames.ALL_USER_TRANSACTION;
import static com.shizzy.moneytransfer.util.CacheNames.SINGLE_TRANSACTION;
import static com.shizzy.moneytransfer.util.CacheNames.TRANSACTIONS;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService implements PaymentService {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    private final StripePaymentProcessor paymentProcessor;
    private final StripeRefundService refundService;
    private final StripeWebhookService webhookService;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceRepository referenceRepository;
    private final TransactionReferenceService referenceService;
    private final KeycloakService keycloakService;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final RefundImpactRecordRepository refundImpactRecordRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(StripeService.class);

    @PostConstruct
    public void init() {
        // Initialize Stripe with API key from properties
        try {
            log.info("Initializing Stripe with API key starting with: {}",
                    stripeApiKey.substring(0, Math.min(8, stripeApiKey.length())) + "...");
            Stripe.apiKey = stripeApiKey;
        } catch (Exception e) {
            log.error("Failed to initialize Stripe: {}", e.getMessage());
        }
    }

    @Override
    public Mono<FlutterwaveResponse> getBanks(String country) {
        return null;
    }

    @Override
    public Mono<ExchangeRateResponse> getExchangeRate(ExchangeRateRequest request) {
        return null;
    }

    @Override
    public GenericResponse<Beneficiary> addBeneficiary(AddBeneficiaryRequest beneficiary) {
        return null;
    }

    @Override
    public GenericResponse<WithdrawalData> withdraw(FlutterwaveWithdrawalRequest withdrawalRequest) {
        throw new UnsupportedOperationException("Stripe does not support withdrawals");
    }

    @Override
    public GenericResponse<List<FeeData>> getFees(double amount, String currency) {
        return null;
    }

    @Override
    public ResponseEntity<String> handleWebhook(WebhookPayload payload) {
        return null;
    }

    @Override
    @CacheEvict(value = { TRANSACTIONS, SINGLE_TRANSACTION, ALL_USER_TRANSACTION }, allEntries = true)
    public ResponseEntity<String> handleWebhook(String payload) {
        return webhookService.handleWebhook(payload);
    }

    @Override
    @Transactional
    @CacheEvict(value = { TRANSACTIONS, SINGLE_TRANSACTION, ALL_USER_TRANSACTION }, allEntries = true)
    public PaymentResponse createPayment(double amount, String email) throws Exception {
        try {

            if (Stripe.apiKey == null) {
                log.error("Stripe API key is null at payment creation time");
                Stripe.apiKey = stripeApiKey; // Attempt to fix
            } else {
                log.info("Using Stripe API key starting with: {}...",
                        Stripe.apiKey.substring(0, Math.min(8, Stripe.apiKey.length())));
            }
            // Generate reference number
            String transactionReference = referenceService.generateUniqueReferenceNumber() + "-STRP";

            // Get user wallet
            String userId = keycloakService.existsUserByEmail(email).getData().getId();
            Wallet wallet = walletRepository.findWalletByCreatedBy(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

            // Create pending transaction
            Transaction transaction = Transaction.builder()
                    .transactionDate(LocalDateTime.now())
                    .transactionType(CREDIT)
                    .operation(DEPOSIT)
                    .wallet(wallet)
                    .currentStatus(PENDING.getValue())
                    .source(STRIPE_DEPOSIT)
                    .description("Wallet Top Up - Self Deposit")
                    .amount(BigDecimal.valueOf(amount))
                    .referenceNumber(transactionReference)
                    .build();
            Transaction savedTransaction = transactionRepository.save(transaction);

            // Create transaction reference
            TransactionReference reference = TransactionReference.builder()
                    .referenceNumber(transactionReference)
                    .creditTransaction(savedTransaction)
                    .build();
            referenceRepository.save(reference);

            // Create checkout session
            Session session = paymentProcessor.createCheckoutSession(
                    email,
                    BigDecimal.valueOf(amount),
                    transactionReference,
                    userId);

            // Return payment response
            return paymentProcessor.generatePaymentResponse(session, transactionReference);
        } catch (Exception e) {
            log.error("Failed to create payment: {} ({})", e.getMessage(), e.getClass().getName());
            if (e.getCause() != null) {
                log.error("  Caused by: {} ({})", e.getCause().getMessage(), e.getCause().getClass().getName());
            }
            throw new PaymentException("Failed to create payment: " + e.getMessage());
        }
    }

    @Override
    public GenericResponse<String> deleteBeneficiary(Integer beneficiaryId) {
        return null;
    }

    @Override
    @Transactional
    public ApiResponse<String> processRefund(RefundRequest refundRequest) throws StripeException {
        try {
            // Find transaction
            Transaction transaction = transactionRepository.findById(refundRequest.transactionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

            // Validate refundable amount
            if (transaction.getRefundableAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Deposit is not refundable");
            }

            BigDecimal refundAmount = transaction.getRefundableAmount();
            log.info("Processing refund of {} for transaction ID: {}, reference: {}",
                    refundAmount, transaction.getTransactionId(), transaction.getReferenceNumber());

            // Update original transaction with optimistic locking to prevent race
            // conditions
            transaction.setRefundableAmount(BigDecimal.ZERO);
            transaction.setRefundStatus(RefundStatus.NON_REFUNDABLE);
            transaction.setRefundDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Create refund impact record
            createRefundImpactRecord(transaction, refundAmount);

            // Create refund transaction
            Transaction refundTransaction = createRefundTransaction(transaction, refundAmount);
            refundTransaction = transactionRepository.save(refundTransaction);

            // Debit wallet with concurrency handling
            walletService.debit(transaction.getWallet(), refundAmount);

            // Process refund with Stripe
            Refund refund = refundService.createRefund(
                    refundRequest.paymentId(),
                    refundAmount,
                    String.valueOf(transaction.getTransactionId()),
                    String.valueOf(refundTransaction.getTransactionId()));

            log.info("Refund successfully processed: {}", refund.getId());

            return ApiResponse.<String>builder()
                    .success(true)
                    .message("Refund request submitted successfully")
                    .data(refund.getStatus())
                    .build();
        } catch (Exception e) {
            log.error("Failed to process refund: {}", e.getMessage());
            throw new PaymentException("Failed to process refund: " + e.getMessage());
        }
    }

    private Transaction createRefundTransaction(Transaction transaction, BigDecimal refundAmount) {
        Transaction refundTransaction = Transaction.builder()
                .amount(refundAmount)
                .currentStatus(PENDING.getValue())
                .transactionDate(LocalDateTime.now())
                .description("Refund for transaction " + transaction.getReferenceNumber())
                .operation(REFUND)
                .transactionType(DEBIT)
                .wallet(transaction.getWallet())
                .build();

        String refundTransactionReference = referenceService.generateUniqueReferenceNumber() + "strp-refund";
        refundTransaction.setReferenceNumber(refundTransactionReference);

        TransactionReference reference = TransactionReference.builder()
                .referenceNumber(refundTransactionReference)
                .debitTransaction(transaction)
                .build();
        referenceRepository.save(reference);

        return refundTransaction;
    }

    private void handleRefundUpdate(String jsonData) throws StripeException {
        JSONObject jsonObject = new JSONObject(jsonData);
        Refund refund = Refund.retrieve(jsonObject.getString("id"));

        if ("failed".equals(refund.getStatus())) {
            final Transaction refundTransaction = updateFailedRefundTransaction(refund);
            updateDepositTransactionAfterFailedRefund(refund, refundTransaction);
        }

    }

    private void updateDepositTransactionAfterFailedRefund(Refund refund, Transaction refundTransaction) {
        final Integer depositId = Integer.parseInt(refund.getMetadata().get("depositId"));

        Transaction depositTransaction = transactionRepository.findById(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit transaction not found"));

        BigDecimal refundAmount = new BigDecimal(refund.getAmount().toString()).divide(new BigDecimal(100));

        depositTransaction.setRefundableAmount(depositTransaction.getRefundableAmount().add(refundAmount));

        if (depositTransaction.getRefundableAmount().compareTo(depositTransaction.getAmount()) == 0) {
            depositTransaction.setRefundStatus(FULLY_REFUNDABLE);
        } else {
            depositTransaction.setRefundStatus(PARTIALLY_REFUNDABLE);
        }

        transactionRepository.save(depositTransaction);

        reverseRefundAndCreditWallet(refundTransaction);
    }

    @NotNull
    private Transaction updateFailedRefundTransaction(Refund refund) {

        Transaction refundTransaction = transactionRepository
                .findById(Integer.parseInt(refund.getMetadata().get("refundId")))
                .orElseThrow(() -> new ResourceNotFoundException("Refund transaction not found"));

        refundTransaction.setCurrentStatus(FAILED.getValue());
        refundTransaction.setNarration(refund.getFailureReason());

        transactionRepository.save(refundTransaction);
        return refundTransaction;
    }

    private void reverseRefundAndCreditWallet(Transaction refundTransaction) {

        Transaction creditTransaction = Transaction.builder()
                .amount(refundTransaction.getAmount())
                .currentStatus(SUCCESS.getValue())
                .transactionDate(LocalDateTime.now())
                .description("Refund reversal for transaction " + refundTransaction.getReferenceNumber())
                .operation(REVERSAL)
                .transactionType(CREDIT)
                .wallet(refundTransaction.getWallet())
                .build();

        Wallet wallet = refundTransaction.getWallet();
        walletService.deposit(wallet, refundTransaction.getAmount());

        transactionRepository.save(creditTransaction);
    }

    private void handleSuccessfulRefund(String jsonData) throws StripeException {
        LOGGER.info("Handling successful refund...");
        JSONObject jsonObject = new JSONObject(jsonData);
        Refund refund = Refund.retrieve(jsonObject.getString("id"));

        Transaction refundTransaction = transactionRepository
                .findById(Integer.parseInt(refund.getMetadata().get("refundId")))
                .orElseThrow(() -> new ResourceNotFoundException("Refund transaction not found"));

        refundTransaction.setCurrentStatus(SUCCESS.getValue());
        transactionRepository.save(refundTransaction);
    }

    /**
     * Creates an audit record for a refund operation
     */
    private void createRefundImpactRecord(Transaction deposit, BigDecimal refundAmount) {
        RefundImpactRecord impactRecord = RefundImpactRecord.builder()
                .depositTransactionId(deposit.getTransactionId())
                .impactAmount(refundAmount.negate()) // Negative because we're reducing refundable amount
                .impactType(RefundImpactType.REFUND)
                .impactDate(LocalDateTime.now())
                .previousRefundableAmount(refundAmount) // Since we're setting to zero, previous was refundAmount
                .newRefundableAmount(BigDecimal.ZERO)
                .relatedTransferAmount(null) // Not applicable for refunds
                .notes("Refund processed via Stripe")
                .build();

        refundImpactRecordRepository.save(impactRecord);
    }

}
