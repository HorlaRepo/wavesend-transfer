package com.shizzy.moneytransfer.observer;

import com.shizzy.moneytransfer.events.TransactionStatusUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogTransactionObserver implements TransactionStatusObserver {
    @Override
    @EventListener
    public void onTransactionStatusChange(TransactionStatusUpdateEvent event) {
        log.info("AUDIT: Transaction {} status changed from {} to {} at {}",
                event.getTransaction().getReferenceNumber(),
                event.getOldStatus(),
                event.getNewStatus(),
                event.getTransaction().getTransactionDate());
    }
}
