package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.AccountLimitDTO;
import com.shizzy.moneytransfer.enums.VerificationLevel;

import java.math.BigDecimal;

public interface AccountLimitService {

    /**
     * Get the current verification level of a user
     */
    VerificationLevel getUserVerificationLevel(String userId);
    
    /**
     * Get account limits for a specific verification level
     */
    AccountLimitDTO getLimitsForLevel(VerificationLevel level);
    
    /**
     * Get account limits for a specific user
     */
    AccountLimitDTO getUserLimits(String userId);
    
    /**
     * Check if a transaction would exceed the daily transaction limit for a user
     */
    boolean wouldExceedDailyLimit(String userId, BigDecimal amount);
    
    /**
     * Check if a new balance would exceed the max balance limit for a user
     */
    boolean wouldExceedBalanceLimit(String userId, BigDecimal newBalance);
    
    /**
     * Check if a deposit would exceed the deposit limit for a user
     */
    boolean wouldExceedDepositLimit(String userId, BigDecimal amount);
    
    /**
     * Check if a withdrawal would exceed the withdrawal limit for a user
     */
    boolean wouldExceedWithdrawalLimit(String userId, BigDecimal amount);
    
    /**
     * Check if a transfer would exceed the transfer limit for a user
     */
    boolean wouldExceedTransferLimit(String userId, BigDecimal amount);
    
    /**
     * Record a transaction for daily limit tracking
     */
    void recordTransaction(String userId, BigDecimal amount);
    
    /**
     * Update account limits for a verification level
     */
    ApiResponse<AccountLimitDTO> updateAccountLimits(VerificationLevel level, AccountLimitDTO limits);
    
    /**
     * Initialize default account limits for all verification levels
     */
    void initializeDefaultLimits();
}