package com.shizzy.moneytransfer.service.payment;

import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.PaymentResponse;
import com.shizzy.moneytransfer.dto.WithdrawalData;
import com.shizzy.moneytransfer.dto.WithdrawalRequestMapper;
import org.springframework.http.ResponseEntity;

public interface PaymentGatewayStrategy {
    PaymentResponse createPayment(double amount, String email) throws Exception;
    GenericResponse<WithdrawalData> processWithdrawal(WithdrawalRequestMapper request);
    ResponseEntity<String> handleWebhook(String payload);
}
