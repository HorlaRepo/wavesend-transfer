package com.shizzy.moneytransfer.events;

import com.shizzy.moneytransfer.model.Transaction;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TransactionStatusUpdateEvent extends ApplicationEvent {
    private final Transaction transaction;
    private final String oldStatus;
    private final String newStatus;

    public TransactionStatusUpdateEvent(Object source, Transaction transaction, String oldStatus, String newStatus) {
        super(source);
        this.transaction = transaction;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}
