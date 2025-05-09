package com.shizzy.moneytransfer.serviceimpl;

import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.MailerSendResponse;
import com.mailersend.sdk.Recipient;
import com.mailersend.sdk.emails.Email;
import com.mailersend.sdk.exceptions.MailerSendException;
import com.shizzy.moneytransfer.dto.ScheduledTransferEmailDto;
import com.shizzy.moneytransfer.dto.TransferEmailDto;
import com.shizzy.moneytransfer.enums.EmailTemplateId;
import com.shizzy.moneytransfer.enums.EmailTemplateName;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailerSendService implements EmailService {

    private final Logger LOGGER = LoggerFactory.getLogger(MailerSendService.class);

    @Value("${mailer-send.api-token}")
    private String apiToken;

    @Override
    public void sendEmail(String to, Map<String, Object> properties, EmailTemplateName templateName, String subject) throws MessagingException {

    }

    @Override
    @Async
    public void sendEmail() {
    }

    @Override
    public void sendCreditTransactionEmail(Transaction transaction, String userEmail, String name, String subject) {
        Email email = buildEmailSettings();
        addPersonalizations(email, transaction, userEmail, name);
        email.setTemplateId(EmailTemplateId.CREDIT_TRANSACTION.getId());
        email.setSubject(subject);
        sendEmail(email);
    }

    @Override
    public void sendDebitTransactionEmail(Transaction transaction, String userEmail, String name, String subject) {
        Email email = buildEmailSettings();
        addPersonalizations(email, transaction, userEmail, name);
        email.setTemplateId(EmailTemplateId.DEBIT_TRANSACTION.getId());
        email.setSubject(subject);
        sendEmail(email);
    }

    private void addPersonalizations(Email email, Transaction transaction, String userEmail, String name) {
        LocalDateTime ldt = LocalDateTime.parse(transaction.getTransactionDate().toString(), DateTimeFormatter.ISO_DATE_TIME);
        email.addRecipient("recipient", userEmail);
        email.addPersonalization("amount", transaction.getAmount().toString() + " " + transaction.getWallet().getCurrency());
        email.addPersonalization("date", transaction.getTransactionDate().toString());
        email.addPersonalization("time", ldt.format(DateTimeFormatter.ofPattern("h:mm a")));
        email.addPersonalization("type", transaction.getTransactionType().toString());
        email.addPersonalization("reference", transaction.getReferenceNumber());
        email.addPersonalization("status", transaction.getCurrentStatus());
        email.addPersonalization("description", transaction.getDescription());
        email.addPersonalization("name", name);
        email.addPersonalization("wallet", transaction.getWallet().getWalletId());
    }

    protected void sendEmail(Email email) {
        MailerSend ms = new MailerSend();
        ms.setToken(apiToken);
        try {
            MailerSendResponse response = ms.emails().send(email);
            LOGGER.info("Email sent successfully");
        } catch (MailerSendException e) {
            LOGGER.error("Error sending email", e);
        }
    }

    private Email buildEmailSettings() {
        Email email = new Email();
        email.setFrom("WaveSend", "MS_i49rSz@nexushrm.com");
        email.addPersonalization("account_name", "WaveSend");
        return email;
    }

    @Override
    public void sendCreditTransferEmail(TransferEmailDto emailDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendCreditTransferEmail'");
    }

    @Override
    public void sendDebitTransferEmail(TransferEmailDto emailDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendDebitTransferEmail'");
    }

    @Override
    public void sendScheduledTransferEmail(ScheduledTransferEmailDto emailDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendScheduledTransferEmail'");
    }

    @Override
    public void sendExecutedTransferEmail(ScheduledTransferEmailDto emailDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendExecutedTransferEmail'");
    }

    @Override
    public void sendCancelledTransferEmail(ScheduledTransferEmailDto emailDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendCancelledTransferEmail'");
    }

    @Override
    public void sendFailedTransferEmail(ScheduledTransferEmailDto emailDto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendFailedTransferEmail'");
    }

   
    

   
}
