package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.dto.Beneficiary;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PaymentService {
    Mono<FlutterwaveResponse> getBanks(String country);
    Mono<ExchangeRateResponse> getExchangeRate(ExchangeRateRequest request);
    GenericResponse<Beneficiary> addBeneficiary(AddBeneficiaryRequest beneficiary);
    GenericResponse<WithdrawalData> withdraw(FlutterwaveWithdrawalRequest withdrawalRequest);
    GenericResponse<List<FeeData>> getFees(double amount, String currency);
    ResponseEntity<String> handleWebhook(WebhookPayload payload);
    ResponseEntity<String> handleWebhook(String payload);
    PaymentResponse createPayment(double amount, String email) throws Exception;
    GenericResponse<String> deleteBeneficiary(Integer beneficiaryId);
    ApiResponse<String> processRefund(RefundRequest refundRequest) throws StripeException;
}
