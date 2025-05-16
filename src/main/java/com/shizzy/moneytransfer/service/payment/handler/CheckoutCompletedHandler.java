package com.shizzy.moneytransfer.service.payment.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shizzy.moneytransfer.dto.TransferInfo;
import com.shizzy.moneytransfer.enums.RefundStatus;
import com.shizzy.moneytransfer.enums.TransactionStatus;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.service.AccountLimitService;
import com.shizzy.moneytransfer.service.TransactionLimitService;
import com.shizzy.moneytransfer.service.TransactionService;
import com.shizzy.moneytransfer.serviceimpl.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.shizzy.moneytransfer.util.CacheNames.ALL_USER_TRANSACTION;
import static com.shizzy.moneytransfer.util.CacheNames.SINGLE_TRANSACTION;
import static com.shizzy.moneytransfer.util.CacheNames.TRANSACTIONS;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutCompletedHandler implements StripeEventHandler {

    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final AccountLimitService accountLimitService;

    @Override
    public boolean canHandle(String eventType) {
        return "checkout.session.completed".equals(eventType);
    }

    @Override
    @Transactional
    @CacheEvict(value = { TRANSACTIONS, SINGLE_TRANSACTION, ALL_USER_TRANSACTION }, allEntries = true)
    public void handleEvent(String eventData) {
        try {
            JsonNode rootNode = objectMapper.readTree(eventData);
            JsonNode objectNode = rootNode.get("object");
            String paymentStatus = objectNode.get("payment_status").asText();

            if ("paid".equals(paymentStatus)) {
                processSuccessfulPayment(objectNode);
            }
        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage());
            throw new RuntimeException("Failed to process payment", e);
        }
    }

    @Transactional
    private void processSuccessfulPayment(JsonNode objectNode) {
        double amountTotal = objectNode.get("amount_total").asLong() / 100.0;
        String sessionId = objectNode.get("id").asText();
        String transactionReference = objectNode.get("metadata").get("transactionReference").asText();
        String paymentIntent = objectNode.get("payment_intent").asText();

        Transaction transaction = transactionService.findByReferenceNumberWithFlaggedReasons(transactionReference);
        
        if (transaction == null || transaction.getCurrentStatus().equals(TransactionStatus.SUCCESS.getValue())) {
            return;
        }

        // Complete transaction
        transactionService.completeDeposit(
                transaction,
                sessionId,
                paymentIntent,
                BigDecimal.valueOf(amountTotal),
                RefundStatus.FULLY_REFUNDABLE);

        // Record the transaction for daily limit tracking
        accountLimitService.recordTransaction(transaction.getWallet().getCreatedBy(), transaction.getAmount());

        // Send notification
        String customerEmail = objectNode.get("customer_details").get("email").asText();
        String customerName = objectNode.get("customer_details").get("name").asText();

        TransferInfo transferInfo = TransferInfo.builder()
                .senderEmail(customerEmail)
                .senderName(customerName)
                .senderId(transaction.getWallet().getCreatedBy())
                .build();

        notificationService.sendDepositNotification(transaction, transferInfo);
    }
}
