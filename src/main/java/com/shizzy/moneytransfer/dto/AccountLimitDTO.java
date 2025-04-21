package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.enums.VerificationLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLimitDTO {
    private VerificationLevel verificationLevel;
    private BigDecimal dailyTransactionLimit;
    private BigDecimal maxWalletBalance;
    private BigDecimal maxDepositAmount;
    private BigDecimal maxWithdrawalAmount;
    private BigDecimal maxTransferAmount;
    
    /**
     * Check if this account has unlimited limits (fully verified)
     */
    public boolean isUnlimited() {
        return verificationLevel == VerificationLevel.FULLY_VERIFIED;
    }
    
    /**
     * Format a limit for display, showing "Unlimited" for null values
     */
    public String getFormattedDailyLimit() {
        return dailyTransactionLimit == null ? "Unlimited" : dailyTransactionLimit.toString();
    }
    
    /**
     * Format max balance for display, showing "Unlimited" for null values
     */
    public String getFormattedMaxBalance() {
        return maxWalletBalance == null ? "Unlimited" : maxWalletBalance.toString();
    }
    
    /**
     * Format max deposit for display, showing "Unlimited" for null values
     */
    public String getFormattedMaxDeposit() {
        return maxDepositAmount == null ? "Unlimited" : maxDepositAmount.toString();
    }
    
    /**
     * Format max withdrawal for display, showing "Unlimited" for null values
     */
    public String getFormattedMaxWithdrawal() {
        return maxWithdrawalAmount == null ? "Unlimited" : maxWithdrawalAmount.toString();
    }
    
    /**
     * Format max transfer for display, showing "Unlimited" for null values
     */
    public String getFormattedMaxTransfer() {
        return maxTransferAmount == null ? "Unlimited" : maxTransferAmount.toString();
    }
}