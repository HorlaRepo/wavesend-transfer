package com.shizzy.moneytransfer.serviceimpl.detection;

import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.model.FlaggedTransactionReason;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.detection.FraudDetectionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DormantAccountRule implements FraudDetectionRule {
    private final TransactionRepository transactionRepository;

    @Override
    public boolean evaluate(Transaction transaction) {
        LocalDateTime lastActive = transactionRepository.findLastTransactionDate(
                transaction.getWallet().getWalletId());

        return lastActive != null &&
                lastActive.isBefore(LocalDateTime.now().minusMonths(6)) &&
                transactionRepository.findRecentTransactions(
                        transaction.getWallet().getWalletId(),
                        TransactionOperation.DEPOSIT,
                        LocalDateTime.now().minusHours(24)
                ).size() > 5;
    }

    @Override
    public FlaggedTransactionReason createReason(Transaction transaction) {
        return FlaggedTransactionReason.builder()
                .reason("Dormant account suddenly activated.")
                .transaction(transaction)
                .build();
    }
}
