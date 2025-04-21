package com.shizzy.moneytransfer.serviceimpl.builder;

import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionStatus;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class TransactionBuilder {
    public Transaction buildTransaction(
            Wallet wallet,
            BigDecimal amount,
            TransactionType type,
            TransactionOperation withdrawal, String referenceNumber,
            String description,
            String narration) {
        return Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .transactionType(type)
                .referenceNumber(referenceNumber)
                .description(description)
                .narration(narration)
                .currentStatus(TransactionStatus.PENDING.getValue())
                .transactionDate(LocalDateTime.now())
                .build();
    }
}
