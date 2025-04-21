package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.TransactionReference;

public interface TransactionReferenceService {
    void saveTransactionReference(TransactionReference transactionReference, String suffix);
    void saveTransactionReference(String referenceNumber);
    ApiResponse<TransactionReference> getTransactionReferenceByReferenceNumber(String referenceNumber);
    String generateUniqueReferenceNumber();
}
