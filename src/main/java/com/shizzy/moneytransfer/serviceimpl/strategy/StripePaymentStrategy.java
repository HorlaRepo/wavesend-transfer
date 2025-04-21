package com.shizzy.moneytransfer.serviceimpl.strategy;

import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.PaymentResponse;
import com.shizzy.moneytransfer.dto.WithdrawalData;
import com.shizzy.moneytransfer.dto.WithdrawalRequestMapper;
import com.shizzy.moneytransfer.service.payment.PaymentGatewayStrategy;
import com.shizzy.moneytransfer.serviceimpl.StripeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Qualifier("stripeStrategy")
@RequiredArgsConstructor
public class StripePaymentStrategy implements PaymentGatewayStrategy {
    private final StripeService stripeService;
    @Override
    public PaymentResponse createPayment(double amount, String email) throws Exception {
        return stripeService.createPayment(amount, email);
    }

    @Override
    public GenericResponse<WithdrawalData> processWithdrawal(WithdrawalRequestMapper request) {
        throw new UnsupportedOperationException("We do not support Stripe withdrawals at the moment");
    }

    @Override
    public ResponseEntity<String> handleWebhook(String payload) {
        return stripeService.handleWebhook(payload);
    }
}
