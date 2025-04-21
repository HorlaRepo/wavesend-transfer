package com.shizzy.moneytransfer.observer;

import com.shizzy.moneytransfer.events.TransactionStatusUpdateEvent;
import org.springframework.context.event.EventListener;

public interface TransactionStatusObserver {
    @EventListener
    void onTransactionStatusChange(TransactionStatusUpdateEvent event);
}
