package com.shizzy.moneytransfer.serviceimpl;



import com.mailersend.sdk.MailerSend;
import com.mailersend.sdk.MailerSendResponse;
import com.mailersend.sdk.emails.Email;
import com.mailersend.sdk.emails.Emails;
import com.mailersend.sdk.exceptions.MailerSendException;
import com.shizzy.moneytransfer.enums.EmailTemplateId;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
class MailerSendServiceTest {

    @Spy
    @InjectMocks
    private MailerSendService mailerSendService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailerSendService, "apiToken", "test-token");
        // Mock the private sendEmail method to prevent actual API calls
        doNothing().when(mailerSendService).sendEmail(any(Email.class));
    }

    @Test
    void testSendDebitTransactionEmail() {
        // Arrange
        Transaction transaction = createTransaction();
        String userEmail = "user@example.com";
        String name = "Test User";
        String subject = "Debit Transaction";

        // Act
        mailerSendService.sendDebitTransactionEmail(transaction, userEmail, name, subject);

        // Assert
        ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
        verify(mailerSendService).sendEmail(emailCaptor.capture());
        
        Email capturedEmail = emailCaptor.getValue();
        assertEquals(EmailTemplateId.DEBIT_TRANSACTION.getId(), ReflectionTestUtils.getField(capturedEmail, "templateId"));
        assertEquals(subject, ReflectionTestUtils.getField(capturedEmail, "subject"));
    }

    private Transaction createTransaction() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setTransactionType(TransactionType.DEBIT);
        transaction.setReferenceNumber("REF123456");
        transaction.setCurrentStatus("COMPLETED");
        transaction.setDescription("Test Transaction");
        
        Wallet wallet = new Wallet();
        wallet.setWalletId("WALLET123");
        wallet.setCurrency("USD");
        transaction.setWallet(wallet);
        
        return transaction;
    }
}