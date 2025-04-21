package com.shizzy.moneytransfer.serviceimpl.command;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.CreateTransactionRequestBody;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.service.transaction.TransactionFactory;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateTransactionCommand implements TransactionCommand<Transaction> {
    private final TransactionRepository transactionRepository;
    private final TransactionFactory transactionFactory;
    private final Wallet wallet;
    private final CreateTransactionRequestBody requestBody;
    private final TransactionType type;
    private final TransactionOperation operation;
    private final String description;
    private final String referenceNumber;

    @Override
    public ApiResponse<Transaction> execute() {
        Transaction transaction = transactionFactory.createTransaction(
                wallet, requestBody, type, operation, description, referenceNumber);

        Transaction savedTransaction = transactionRepository.save(transaction);

        return ApiResponse.<Transaction>builder()
                .success(true)
                .message("Transaction created successfully")
                .data(savedTransaction)
                .build();
    }
}
