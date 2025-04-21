package com.shizzy.moneytransfer.serviceimpl.detection;

import com.shizzy.moneytransfer.model.FlaggedTransactionReason;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.WalletService;
import com.shizzy.moneytransfer.service.detection.FraudDetectionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class UnusualAmountRule implements FraudDetectionRule {
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;

    @Override
    public boolean evaluate(Transaction transaction) {
        if(walletService.isWalletNew(transaction.getWallet().getWalletId())) {
            return false;
        }
        BigDecimal averageAmount = transactionRepository.getAverageTransactionAmount(
                transaction.getWallet().getWalletId());
        BigDecimal deviation = transaction.getAmount().subtract(averageAmount).abs();
        return deviation.compareTo(averageAmount.multiply(new BigDecimal("0.5"))) > 0;
    }

    @Override
    public FlaggedTransactionReason createReason(Transaction transaction) {
        return FlaggedTransactionReason.builder()
                .reason("Unusual amount detected.")
                .transaction(transaction)
                .build();
    }
}
