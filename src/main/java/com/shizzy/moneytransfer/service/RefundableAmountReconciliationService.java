package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.enums.RefundImpactType;
import com.shizzy.moneytransfer.enums.RefundStatus;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.model.RefundImpactRecord;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.RefundImpactRecordRepository;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundableAmountReconciliationService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final RefundImpactRecordRepository refundImpactRecordRepository;

    /**
     * Daily reconciliation of refundable amounts
     * Runs at 2 AM every day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void reconcileRefundableAmounts() {
        log.info("Starting daily refundable amount reconciliation");
        
        List<Wallet> wallets = walletRepository.findAll();
        log.info("Processing {} wallets for reconciliation", wallets.size());
        
        int reconciliationsPerformed = 0;
        
        for (Wallet wallet : wallets) {
            boolean reconciled = reconcileWalletRefundableAmounts(wallet);
            if (reconciled) {
                reconciliationsPerformed++;
            }
        }
        
        log.info("Completed daily reconciliation. Fixed inconsistencies in {} wallets", 
                reconciliationsPerformed);
    }
    
    /**
     * Reconciles refundable amounts for a specific wallet
     * @return true if any reconciliation was needed and performed
     */
    private boolean reconcileWalletRefundableAmounts(Wallet wallet) {
        // Find all deposit transactions for this wallet
        List<Transaction> deposits = transactionRepository.findByWalletIdAndOperation(
                wallet.getId(), TransactionOperation.DEPOSIT);
        
        boolean reconciliationPerformed = false;
        
        for (Transaction deposit : deposits) {
            // Skip transactions that are already marked as non-refundable
            if (deposit.getRefundStatus() == RefundStatus.NON_REFUNDABLE) {
                continue;
            }
            
            // Check if refundable amount is correct according to impact records
            BigDecimal expectedRefundableAmount = calculateExpectedRefundableAmount(deposit);
            
            if (!deposit.getRefundableAmount().equals(expectedRefundableAmount)) {
                log.warn("Inconsistency found in deposit ID {}: recorded refundable amount is {} but should be {}", 
                        deposit.getTransactionId(), deposit.getRefundableAmount(), expectedRefundableAmount);
                
                // Create reconciliation record
                RefundImpactRecord reconciliationRecord = RefundImpactRecord.builder()
                        .depositTransactionId(deposit.getTransactionId())
                        .impactType(RefundImpactType.RECONCILIATION)
                        .impactAmount(expectedRefundableAmount.subtract(deposit.getRefundableAmount()))
                        .previousRefundableAmount(deposit.getRefundableAmount())
                        .newRefundableAmount(expectedRefundableAmount)
                        .impactDate(LocalDateTime.now())
                        .notes("System reconciliation - fixed inconsistent refundable amount")
                        .build();
                
                refundImpactRecordRepository.save(reconciliationRecord);
                
                // Update the transaction
                deposit.setRefundableAmount(expectedRefundableAmount);
                
                // Update status based on refundable amount
                if (expectedRefundableAmount.compareTo(BigDecimal.ZERO) == 0) {
                    deposit.setRefundStatus(RefundStatus.NON_REFUNDABLE);
                } else if (expectedRefundableAmount.compareTo(deposit.getAmount()) < 0) {
                    deposit.setRefundStatus(RefundStatus.PARTIALLY_REFUNDABLE);
                } else {
                    deposit.setRefundStatus(RefundStatus.FULLY_REFUNDABLE);
                }
                
                transactionRepository.save(deposit);
                reconciliationPerformed = true;
            }
        }
        
        return reconciliationPerformed;
    }
    
    /**
     * Calculates the expected refundable amount based on impact records
     */
    private BigDecimal calculateExpectedRefundableAmount(Transaction deposit) {
        // Start with the original deposit amount
        BigDecimal expectedAmount = deposit.getAmount();
        
        // Subtract all impacts from the record
        List<RefundImpactRecord> impacts = refundImpactRecordRepository
                .findByDepositTransactionId(deposit.getTransactionId());
        
        for (RefundImpactRecord impact : impacts) {
            // Impact amount is stored as negative (reductions) or positive (increases)
            expectedAmount = expectedAmount.add(impact.getImpactAmount());
        }
        
        // Cannot be negative
        return expectedAmount.max(BigDecimal.ZERO);
    }
}