package com.shizzy.moneytransfer.dao;

import com.shizzy.moneytransfer.dto.PaymentResponse;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;

public interface PaymentService {
    PaymentResponse createPayment(double amount, String userEmail) throws StripeException;
    ResponseEntity<String> handleWebhook(String payload);
}
