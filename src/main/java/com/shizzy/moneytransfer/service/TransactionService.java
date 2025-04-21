package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.enums.RefundStatus;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionStatus;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {
     ApiResponse<PagedTransactionResponse> getAllTransactions(int page, int size);

     ApiResponse<List<TransactionResponse>> getTransactionByReferenceNumber(String referenceNumber);

     ApiResponse<TransactionResponse> getTransactionById(Integer id);

     ApiResponse<TransactionResponse> updateTransaction(Integer id, TransactionStatusDTO status);

     ApiResponse<PagedTransactionResponse> getTransactionsByWallet(String walletId, int page, int size);

     ApiResponse<PagedTransactionResponse> getUserTransactionsByDate(TransactionsByDateRequest request);

     ApiResponse<PagedTransactionResponse> searchTransactions(String inputString, String sortOrder, String searchQuery, int page, int size);

     ApiResponse<TransactionResponse> updateTransactionStatus(String referenceNumber, UpdateTransactionRequest request);

     String getTransactionStatus(String referenceNumber);

     Transaction findByReferenceNumber(String referenceNumber);

     Transaction findById(Integer id);

     void completeDeposit(Transaction transaction, String sessionId, String providerId,
                          BigDecimal amount, RefundStatus refundStatus);

     Transaction createReversalTransaction(Wallet wallet, BigDecimal amount, String description,
                                           TransactionOperation operation);

     Transaction createRefundTransaction(Transaction originalTransaction, BigDecimal amount, String referenceNumber);

     void updateTransactionStatus(Integer transactionId, TransactionStatus status, String narration);

     void restoreRefundableAmount(Integer depositId, BigDecimal amount);

     ApiResponse<TransactionFee> getTransactionFee(double amount);

     ApiResponse<PagedTransactionResponse> getTransactionsByFilter(Long walletId, String filter, String startDate, String endDate, int pageNumber, int pageSize);

     Transaction createTransaction(Wallet wallet, CreateTransactionRequestBody requestBody, TransactionType transactionType, TransactionOperation operation, String description, String referenceNumber);
}