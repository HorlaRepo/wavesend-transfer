package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.exception.FraudulentTransactionException;
import com.shizzy.moneytransfer.model.Transaction;

public interface TransactionMonitoringService {
    void monitorTransaction(Transaction transaction) throws FraudulentTransactionException;
}
