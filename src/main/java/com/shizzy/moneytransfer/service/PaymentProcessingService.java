package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;

public interface PaymentProcessingService {
    GenericResponse<WithdrawalData> withdraw(WithdrawalRequestMapper requestMapper);
    ResponseEntity<String> handleFlutterwaveWebhook(WebhookPayload payload);
    ApiResponse<TransactionResponseDTO> sendMoney(@NotNull CreateTransactionRequestBody requestBody);
    PaymentResponse createStripePayment(double amount, String userEmail) throws Exception;
    ResponseEntity<String> handleStripeWebhook(String payload);
}
