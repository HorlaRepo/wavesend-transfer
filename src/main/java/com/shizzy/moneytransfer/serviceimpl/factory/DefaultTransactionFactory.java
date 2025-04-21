package com.shizzy.moneytransfer.serviceimpl.factory;

import com.shizzy.moneytransfer.dto.CreateTransactionRequestBody;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionStatus;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.service.transaction.TransactionFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DefaultTransactionFactory implements TransactionFactory {
    @Override
    public Transaction createTransaction(
            Wallet wallet,
            CreateTransactionRequestBody request,
            TransactionType type,
            TransactionOperation operation,
            String description,
            String referenceNumber) {
        return Transaction.builder()
                .wallet(wallet)
                .amount(request.amount())
                .transactionType(type)
                .narration(request.narration())
                .currentStatus(TransactionStatus.PENDING.getValue())
                .operation(operation)
                .description(description)
                .referenceNumber(referenceNumber)
                .transactionDate(LocalDateTime.now())
                .build();
    }
}
