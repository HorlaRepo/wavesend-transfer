package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.dto.TransactionNotification;
import com.shizzy.moneytransfer.dto.TransferInfo;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.kafka.NotificationProducer;
import com.shizzy.moneytransfer.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationProducer notificationProducer;

    public void sendDepositNotification(Transaction transaction, TransferInfo transferInfo) {
        TransactionNotification notification = TransactionNotification.builder()
                .operation(TransactionOperation.DEPOSIT)
                .creditTransaction(transaction)
                .transferInfo(transferInfo)
                .build();

        notificationProducer.sendNotification("notifications", notification);
    }

    public void sendRefundNotification(Transaction refundTransaction, TransferInfo transferInfo) {
        TransactionNotification notification = TransactionNotification.builder()
                .operation(TransactionOperation.REFUND)
                .debitTransaction(refundTransaction)
                .transferInfo(transferInfo)
                .build();

        notificationProducer.sendNotification("notifications", notification);
    }
}
