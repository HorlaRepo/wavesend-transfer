package com.shizzy.moneytransfer.service;

import org.springframework.http.ResponseEntity;

public interface WebhookHandler {
    ResponseEntity<String> handleWebhook(String payload);
}
