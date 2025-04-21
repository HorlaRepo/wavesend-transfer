package com.shizzy.moneytransfer.service.payment.handler;

public interface StripeEventHandler {
    boolean canHandle(String eventType);
    void handleEvent(String eventData);
}
