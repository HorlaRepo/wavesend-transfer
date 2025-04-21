package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferEmailDto {
    private Transaction transaction;
    private String senderEmail;
    private String receiverEmail;
    private String senderName;
    private String receiverName;
    private String subject;
    
    /**
     * Gets the appropriate email address based on email type
     * @param isCredit whether this is for credit email (to receiver) or debit email (to sender)
     * @return the appropriate recipient email address
     */
    public String getRecipientEmail(boolean isCredit) {
        return isCredit ? receiverEmail : senderEmail;
    }
}