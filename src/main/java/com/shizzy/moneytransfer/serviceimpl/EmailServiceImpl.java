package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.dto.ScheduledTransferEmailDto;
import com.shizzy.moneytransfer.dto.TransferEmailDto;
import com.shizzy.moneytransfer.enums.EmailTemplateName;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

import static org.springframework.mail.javamail.MimeMessageHelper.*;

@Service
@RequiredArgsConstructor
@Qualifier("EmailServiceImpl")
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Async
    @Override
    public void sendEmail(
            String to,
           Map<String, Object> properties,
            EmailTemplateName templateName,
            String subject
    ) throws MessagingException{
        String template = "";
        if(templateName == null){
            template = "confirm-email";
        } else {
            template = templateName.getName();
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MULTIPART_MODE_MIXED,
                "utf-8"
        );
        Context context = new Context();
        context.setVariables(properties);

        helper.setFrom("cc@shizzy.dev");
        helper.setTo(to);
        helper.setSubject(subject);

        String htmlTemplate = templateEngine.process(template, context);
        helper.setText(htmlTemplate, true);

        mailSender.send(message);

    }

    @Override
    public void sendEmail() {

    }

    @Override
    public void sendCreditTransactionEmail(Transaction transaction, String userEmail, String name, String subject) {

    }

    @Override
    public void sendDebitTransactionEmail(Transaction transaction, String userEmail, String name, String subject) {

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
