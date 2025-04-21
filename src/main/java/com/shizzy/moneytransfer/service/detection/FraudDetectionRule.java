package com.shizzy.moneytransfer.service.detection;

import com.shizzy.moneytransfer.model.FlaggedTransactionReason;
import com.shizzy.moneytransfer.model.Transaction;

public interface FraudDetectionRule {
    boolean evaluate(Transaction transaction);
    FlaggedTransactionReason createReason(Transaction transaction);
}
