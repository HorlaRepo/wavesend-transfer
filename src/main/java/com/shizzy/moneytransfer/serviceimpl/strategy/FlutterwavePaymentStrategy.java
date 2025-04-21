package com.shizzy.moneytransfer.serviceimpl.strategy;

import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.FlutterwaveWithdrawalRequest;
import com.shizzy.moneytransfer.dto.PaymentResponse;
import com.shizzy.moneytransfer.dto.WithdrawalData;
import com.shizzy.moneytransfer.dto.WithdrawalRequestMapper;
import com.shizzy.moneytransfer.service.payment.PaymentGatewayStrategy;
import com.shizzy.moneytransfer.serviceimpl.FlutterwaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Qualifier("flutterwaveStrategy")
@RequiredArgsConstructor
public class FlutterwavePaymentStrategy implements PaymentGatewayStrategy {
    private final FlutterwaveService flutterwaveService;
    @Override
    public PaymentResponse createPayment(double amount, String email) throws Exception {
        return flutterwaveService.createPayment(amount, email);
    }

    @Override
    public GenericResponse<WithdrawalData> processWithdrawal(WithdrawalRequestMapper request) {
        // Convert WithdrawalRequestMapper to FlutterwaveWithdrawalRequest
        FlutterwaveWithdrawalRequest withdrawalRequest = flutterwaveService.buildFlutterwaveWithdrawalRequest(
                request.getWithdrawalInfo(),
                request.getReferenceNumber()

        );
        return flutterwaveService.withdraw(withdrawalRequest);
    }

    @Override
    public ResponseEntity<String> handleWebhook(String payload) {
        return flutterwaveService.handleWebhook(payload);
    }
}
