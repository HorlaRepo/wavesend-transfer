package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.client.ConversationMessage;
import com.shizzy.moneytransfer.model.UserBeneficiary;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ConversationState {
    private String userId;
    private TransactionStage stage = TransactionStage.NONE;
    private TransactionIntent intent = TransactionIntent.UNKNOWN;
    private List<ConversationMessage> messages = new ArrayList<>();
    
    // Current transaction details
    private BigDecimal amount;
    private String recipientName;
    private String note;
    private LocalDateTime scheduledDateTime;
    private String bankName;
    private String accountNumber;
    
    // Stored context
    private List<UserBeneficiary> matchingBeneficiaries;
    private String selectedBeneficiaryEmail;
    private String transferToken;
    private String scheduledTransferToken;
    private String withdrawalToken;
    
    public enum TransactionStage {
        NONE,
        SELECTING_BENEFICIARY,
        ENTERING_RECIPIENT_EMAIL,
        CONFIRMING_TRANSACTION,
        ENTERING_OTP,
        TRANSACTION_COMPLETED,
        TRANSACTION_FAILED
    }
    
    public void addMessage(String role, String content) {
        if (messages.size() >= 10) {
            messages.remove(0);
        }
        messages.add(new ConversationMessage(role, content));
    }

    private LocalDateTime otpSentAt;
    private int otpAttempts = 0;

    /**
     * Increments the OTP attempt counter
     */
    public void incrementOtpAttempts() {
        this.otpAttempts++;
    }
    
    /**
     * Resets the OTP attempt counter
     */
    public void resetOtpAttempts() {
        this.otpAttempts = 0;
    }
    
    public void reset() {
        stage = TransactionStage.NONE;
        intent = TransactionIntent.UNKNOWN;
        amount = null;
        recipientName = null;
        note = null;
        scheduledDateTime = null;
        bankName = null;
        accountNumber = null;
        matchingBeneficiaries = null;
        selectedBeneficiaryEmail = null;
        transferToken = null;
        scheduledTransferToken = null;
        withdrawalToken = null;
        this.otpAttempts = 0;
        this.otpSentAt = null;
    }

   
}