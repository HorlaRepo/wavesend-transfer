package com.shizzy.moneytransfer.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.exception.PaymentException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.TransactionReference;
import com.shizzy.moneytransfer.model.Wallet;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService implements PaymentService {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    private final StripePaymentProcessor paymentProcessor;
    private final StripeRefundService refundService;
    private final StripeWebhookService webhookService;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceRepository referenceRepository;
    private final TransactionReferenceService referenceService;
    private final KeycloakService keycloakService;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private static final Logger LOGGER = LoggerFactory.getLogger(StripeService.class);

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
    public ResponseEntity<String> handleWebhook(String payload) {
        return webhookService.handleWebhook(payload);
//        Stripe.apiKey = stripeApiKey;
//        Event event;
//        try {
//            event = ApiResource.GSON.fromJson(String.valueOf(payload), Event.class);
//        } catch (JsonSyntaxException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//
//        if ("checkout.session.completed".equals(event.getType())) {
//            String jsonData = event.getData().toJson();
//            System.out.println(jsonData);
//            handleStripeDeposit(jsonData);
//        }
//
//        if ("charge.refund.updated".equals(event.getType())) {
//            try {
//                handleRefundUpdate(payload);
//            } catch (StripeException e) {
//                LOGGER.error("Error processing stripe refund: {}", e.getMessage());
//            }
//        }
//
//        if("charge.refunded".equals(event.getType())) {
//            try {
//                handleSuccessfulRefund(payload);
//            } catch (StripeException e) {
//                LOGGER.error("Error processing stripe refund: {}", e.getMessage());
//            }
//        }
//
//        return ResponseEntity.ok("Success");
    }

//    private void handleStripeDeposit(String jsonData) {
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode rootNode = mapper.readTree(jsonData);
//            JsonNode objectNode = rootNode.get("object");
//            String paymentStatus = objectNode.get("payment_status").asText();
//
//            if ("paid".equals(paymentStatus)) {
//                double amountTotal = objectNode.get("amount_total").asLong() / 100.0;
//                String sessionId = objectNode.get("id").asText();
//                String transactionReference = objectNode.get("metadata").get("transactionReference").asText();
//                String paymentIntent = objectNode.get("payment_intent").asText();
//
//                if (!transactionReference.isEmpty()) {
//                    Transaction transaction = transactionRepository.findTransactionByReferenceNumber(transactionReference).get(0);
//
//                    if (transaction.getCurrentStatus().equals(SUCCESS.getValue())) {
//                        return;
//                    }
//
//                    if (transaction.getCurrentStatus().equals(PENDING.getValue())) {
//                        transaction.setCurrentStatus(SUCCESS.getValue());
//                        transaction.setSessionId(sessionId);
//                        transaction.setRefundableAmount(BigDecimal.valueOf(amountTotal));
//                        transaction.setRefundStatus(FULLY_REFUNDABLE);
//                        transaction.setProviderId(paymentIntent);
//
//                        Wallet wallet = transaction.getWallet();
//                        walletService.deposit(wallet, BigDecimal.valueOf(amountTotal));
//
//                        transactionRepository.save(transaction);
//
//                        String customerEmail = objectNode.get("customer_details").get("email").asText();
//                        String customerName = objectNode.get("customer_details").get("name").asText();
//
//                        TransferInfo transferInfo = TransferInfo.builder()
//                                .senderEmail(customerEmail)
//                                .senderName(customerName)
//                                .senderId(transaction.getWallet().getCreatedBy())
//                                .build();
//
//                        TransactionNotification notification = TransactionNotification.builder()
//                                .operation(DEPOSIT)
//                                .creditTransaction(transaction)
//                                .transferInfo(transferInfo)
//                                .build();
//
//                        notificationProducer.sendNotification("notifications",notification);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            LOGGER.error("Error processing stripe deposit: {}", e.getMessage());
//        }
//    }

    @Override
    @Transactional
    public PaymentResponse createPayment(double amount, String email) throws Exception {
        try {
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
                    userId
            );

            // Return payment response
            return paymentProcessor.generatePaymentResponse(session, transactionReference);
        } catch (Exception e) {
            log.error("Failed to create payment: {}", e.getMessage());
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

            // Update original transaction
            transaction.setRefundableAmount(BigDecimal.ZERO);
            transaction.setRefundStatus(NON_REFUNDABLE);
            transactionRepository.save(transaction);

            // Create refund transaction
            Transaction refundTransaction = createRefundTransaction(transaction, refundAmount);

            // Debit wallet
            walletService.debit(transaction.getWallet(), refundAmount);

            // Process refund with Stripe
            Refund refund = refundService.createRefund(
                    refundRequest.paymentId(),
                    refundAmount,
                    String.valueOf(transaction.getTransactionId()),
                    String.valueOf(refundTransaction.getTransactionId())
            );

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

        if("failed".equals(refund.getStatus())) {
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

        Transaction refundTransaction = transactionRepository.findById(Integer.parseInt(refund.getMetadata().get("refundId")))
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

        Transaction refundTransaction = transactionRepository.findById(Integer.parseInt(refund.getMetadata().get("refundId")))
                .orElseThrow(() -> new ResourceNotFoundException("Refund transaction not found"));

        refundTransaction.setCurrentStatus(SUCCESS.getValue());
        transactionRepository.save(refundTransaction);
    }

}
