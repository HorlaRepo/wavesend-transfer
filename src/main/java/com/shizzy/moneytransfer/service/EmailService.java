package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.dto.ScheduledTransferEmailDto;
import com.shizzy.moneytransfer.dto.TransferEmailDto;
import com.shizzy.moneytransfer.enums.EmailTemplateName;
import com.shizzy.moneytransfer.model.Transaction;
import jakarta.mail.MessagingException;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;

public interface EmailService {
    void sendEmail(String to, Map<String, Object> properties, EmailTemplateName templateName, String subject) throws MessagingException;
    void sendEmail();

    @Async
    void sendCreditTransactionEmail(Transaction transaction, String userEmail, String name, String subject);

    @Async
    void sendDebitTransactionEmail(Transaction transaction, String userEmail, String name, String subject);
    
    @Async
    void sendCreditTransferEmail(TransferEmailDto emailDto);
    
    @Async
    void sendDebitTransferEmail(TransferEmailDto emailDto);
    
    // New methods for scheduled transfers
    @Async
    void sendScheduledTransferEmail(ScheduledTransferEmailDto emailDto);
    
    @Async
    void sendExecutedTransferEmail(ScheduledTransferEmailDto emailDto);
    
    @Async
    void sendCancelledTransferEmail(ScheduledTransferEmailDto emailDto);
    
    @Async
    void sendFailedTransferEmail(ScheduledTransferEmailDto emailDto);
}
