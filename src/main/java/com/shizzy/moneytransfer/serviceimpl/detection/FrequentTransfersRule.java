package com.shizzy.moneytransfer.serviceimpl.detection;

import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.model.FlaggedTransactionReason;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.detection.FraudDetectionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FrequentTransfersRule implements FraudDetectionRule {
    private final TransactionRepository transactionRepository;

    @Override
    public boolean evaluate(Transaction transaction) {
        List<Transaction> transactions = transactionRepository.findRecentTransactions(
                transaction.getWallet().getWalletId(),
                TransactionOperation.TRANSFER,
                LocalDateTime.now().minusHours(24));
        return transactions.size() > 10;
    }

    @Override
    public FlaggedTransactionReason createReason(Transaction transaction) {
        return FlaggedTransactionReason.builder()
                .reason("Frequent transfers detected.")
                .transaction(transaction)
                .build();
    }
}
