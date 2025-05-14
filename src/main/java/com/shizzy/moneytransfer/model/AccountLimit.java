package com.shizzy.moneytransfer.model;

import com.shizzy.moneytransfer.enums.VerificationLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Table(name = "account_limits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private VerificationLevel verificationLevel;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal dailyTransactionLimit;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal maxWalletBalance;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal maxDepositAmount;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal maxWithdrawalAmount;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal maxTransferAmount;


    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String lastModifiedBy;
    
    /**
     * Check if a transaction amount exceeds the limit
     */
    public boolean isTransactionExceedingLimit(BigDecimal amount) {
        return dailyTransactionLimit != null && 
               dailyTransactionLimit.compareTo(BigDecimal.ZERO) > 0 && 
               amount.compareTo(dailyTransactionLimit) > 0;
    }
    
    /**
     * Check if a new balance would exceed the wallet balance limit
     */
    public boolean isBalanceExceedingLimit(BigDecimal balance) {
        return maxWalletBalance != null && 
               maxWalletBalance.compareTo(BigDecimal.ZERO) > 0 && 
               balance.compareTo(maxWalletBalance) > 0;
    }
    
    /**
     * Check if a deposit amount exceeds the limit
     */
    public boolean isDepositExceedingLimit(BigDecimal amount) {
        return maxDepositAmount != null && 
               maxDepositAmount.compareTo(BigDecimal.ZERO) > 0 && 
               amount.compareTo(maxDepositAmount) > 0;
    }
    
    /**
     * Check if a withdrawal amount exceeds the limit
     */
    public boolean isWithdrawalExceedingLimit(BigDecimal amount) {
        return maxWithdrawalAmount != null && 
               maxWithdrawalAmount.compareTo(BigDecimal.ZERO) > 0 && 
               amount.compareTo(maxWithdrawalAmount) > 0;
    }
    
    /**
     * Check if a transfer amount exceeds the limit
     */
    public boolean isTransferExceedingLimit(BigDecimal amount) {
        return maxTransferAmount != null && 
               maxTransferAmount.compareTo(BigDecimal.ZERO) > 0 && 
               amount.compareTo(maxTransferAmount) > 0;
    }
}