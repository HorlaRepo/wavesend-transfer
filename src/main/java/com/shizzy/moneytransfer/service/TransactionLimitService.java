package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.AccountLimitDTO;
import com.shizzy.moneytransfer.exception.TransactionLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLimitService {

    private final AccountLimitService accountLimitService;

    /**
     * Check if a deposit would exceed the user's limits
     */
    public void validateDeposit(String userId, BigDecimal amount) {
        if (accountLimitService.wouldExceedDepositLimit(userId, amount)) {
            AccountLimitDTO limits = accountLimitService.getUserLimits(userId);
            throw new TransactionLimitExceededException(
                    "Deposit amount exceeds your limit of " + limits.getMaxDepositAmount());
        }
        
        if (accountLimitService.wouldExceedDailyLimit(userId, amount)) {
            AccountLimitDTO limits = accountLimitService.getUserLimits(userId);
            throw new TransactionLimitExceededException(
                    "This transaction would exceed your daily transaction limit of " + limits.getDailyTransactionLimit());
        }
    }
    
    /**
     * Check if a withdrawal would exceed the user's limits
     */
    public void validateWithdrawal(String userId, BigDecimal amount) {
        if (accountLimitService.wouldExceedWithdrawalLimit(userId, amount)) {
            AccountLimitDTO limits = accountLimitService.getUserLimits(userId);
            throw new TransactionLimitExceededException(
                    "Withdrawal amount exceeds your limit of " + limits.getMaxWithdrawalAmount());
        }
        
        if (accountLimitService.wouldExceedDailyLimit(userId, amount)) {
            AccountLimitDTO limits = accountLimitService.getUserLimits(userId);
            throw new TransactionLimitExceededException(
                    "This transaction would exceed your daily transaction limit of " + limits.getDailyTransactionLimit());
        }
    }
    
    /**
     * Check if a transfer would exceed the user's limits
     */
    public void validateTransfer(String userId, BigDecimal amount) {
        if (accountLimitService.wouldExceedTransferLimit(userId, amount)) {
            AccountLimitDTO limits = accountLimitService.getUserLimits(userId);
            throw new TransactionLimitExceededException(
                    "Transfer amount exceeds your limit of " + limits.getMaxTransferAmount());
        }
        
        if (accountLimitService.wouldExceedDailyLimit(userId, amount)) {
            AccountLimitDTO limits = accountLimitService.getUserLimits(userId);
            throw new TransactionLimitExceededException(
                    "This transaction would exceed your daily transaction limit of " + limits.getDailyTransactionLimit());
        }
    }
    
    /**
     * Check if a new balance would exceed the user's wallet balance limit
     */
    public void validateNewBalance(String userId, BigDecimal newBalance) {
        if (accountLimitService.wouldExceedBalanceLimit(userId, newBalance)) {
            AccountLimitDTO limits = accountLimitService.getUserLimits(userId);
            throw new TransactionLimitExceededException(
                    "This transaction would exceed your maximum wallet balance limit of " + limits.getMaxWalletBalance());
        }
    }
}