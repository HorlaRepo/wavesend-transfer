package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.dto.TransactionNotification;
import com.shizzy.moneytransfer.dto.TransferInfo;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.kafka.NotificationProducer;
import com.shizzy.moneytransfer.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationProducer notificationProducer;

    @Captor
    private ArgumentCaptor<TransactionNotification> notificationCaptor;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationProducer);
    }

    @Test
    void sendDepositNotification_ShouldSendCorrectNotification() {
        // Arrange
        Transaction transaction = new Transaction();
        TransferInfo transferInfo = new TransferInfo();

        // Act
        notificationService.sendDepositNotification(transaction, transferInfo);

        // Assert
        verify(notificationProducer).sendNotification(eq("notifications"), notificationCaptor.capture());

        TransactionNotification capturedNotification = notificationCaptor.getValue();
        assertEquals(TransactionOperation.DEPOSIT, capturedNotification.getOperation());
        assertEquals(transaction, capturedNotification.getCreditTransaction());
        assertEquals(transferInfo, capturedNotification.getTransferInfo());
    }

    @Test
    void sendRefundNotification_ShouldSendCorrectNotification() {
        // Arrange
        Transaction refundTransaction = new Transaction();
        TransferInfo transferInfo = new TransferInfo();

        // Act
        notificationService.sendRefundNotification(refundTransaction, transferInfo);

        // Assert
        verify(notificationProducer).sendNotification(eq("notifications"), notificationCaptor.capture());

        TransactionNotification capturedNotification = notificationCaptor.getValue();
        assertEquals(TransactionOperation.REFUND, capturedNotification.getOperation());
        assertEquals(refundTransaction, capturedNotification.getDebitTransaction());
        assertEquals(transferInfo, capturedNotification.getTransferInfo());
    }
}