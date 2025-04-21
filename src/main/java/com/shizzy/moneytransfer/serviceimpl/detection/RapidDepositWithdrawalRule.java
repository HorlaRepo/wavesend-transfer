package com.shizzy.moneytransfer.serviceimpl.detection;

import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.model.FlaggedTransactionReason;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.detection.FraudDetectionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RapidDepositWithdrawalRule implements FraudDetectionRule {
    private final TransactionRepository transactionRepository;

    @Override
    public boolean evaluate(Transaction transaction) {
        LocalDateTime recentDate = LocalDateTime.now().minusHours(24);
        List<Transaction> recentDeposits = transactionRepository.findRecentTransactions(
                transaction.getWallet().getWalletId(), TransactionOperation.DEPOSIT, recentDate);
        List<Transaction> recentWithdrawals = transactionRepository.findRecentTransactions(
                transaction.getWallet().getWalletId(), TransactionOperation.WITHDRAWAL, recentDate);

        List<Transaction> recentTransactions = new ArrayList<>();
        recentTransactions.addAll(recentDeposits);
        recentTransactions.addAll(recentWithdrawals);

        return recentTransactions.size() > 5 &&
                recentTransactions.stream().allMatch(t -> t.getAmount().compareTo(new BigDecimal("5000")) > 0);
    }

    @Override
    public FlaggedTransactionReason createReason(Transaction transaction) {
        return FlaggedTransactionReason.builder()
                .reason("Rapid deposit and withdrawal detected.")
                .transaction(transaction)
                .build();
    }


}