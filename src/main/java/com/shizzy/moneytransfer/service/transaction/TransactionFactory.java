package com.shizzy.moneytransfer.service.transaction;

import com.shizzy.moneytransfer.dto.CreateTransactionRequestBody;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;

public interface TransactionFactory {
    Transaction createTransaction(
            Wallet wallet,
            CreateTransactionRequestBody request,
            TransactionType type,
            TransactionOperation operation,
            String description,
            String referenceNumber
    );
}
