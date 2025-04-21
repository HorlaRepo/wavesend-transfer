package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.exception.FraudulentTransactionException;
import com.shizzy.moneytransfer.model.FlaggedTransactionReason;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.repository.FlaggedTransactionReasonRepository;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.TransactionMonitoringService;
import com.shizzy.moneytransfer.service.WalletService;
import com.shizzy.moneytransfer.service.detection.FraudDetectionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionMonitoringServiceImpl implements TransactionMonitoringService {

    private final TransactionRepository transactionRepository;
    private final FlaggedTransactionReasonRepository flaggedTransactionReasonRepository;
    private final WalletService walletService;
    private final List<FraudDetectionRule> fraudDetectionRules;

    @Override
    public void monitorTransaction(Transaction transaction) throws FraudulentTransactionException {
        List<FlaggedTransactionReason> reasons = new ArrayList<>();

        // Apply all fraud detection rules
        for (FraudDetectionRule rule : fraudDetectionRules) {
            if (rule.evaluate(transaction)) {
                reasons.add(rule.createReason(transaction));
            }
        }

        if (!reasons.isEmpty()) {
            flaggedTransactionReasonRepository.saveAll(reasons);
            transaction.setFlagged(true);
            transaction.getFlaggedTransactionReasons().addAll(reasons);
            transactionRepository.save(transaction);

            // If more than 2 rules triggered, flag the entire wallet
            if (reasons.size() > 2) {
                walletService.flagWallet(transaction.getWallet().getWalletId());
            }

            throw new FraudulentTransactionException("Operation failed. Please contact support for assistance.");
        }
    }
}
