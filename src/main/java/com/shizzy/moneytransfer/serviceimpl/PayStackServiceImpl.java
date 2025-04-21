package com.shizzy.moneytransfer.serviceimpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.dto.Beneficiary;
import com.shizzy.moneytransfer.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class PayStackServiceImpl implements PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayStackServiceImpl.class);


    private final WebClient webClient;

    @Value("${paystack.api.key}")
    private String apiKey;

    @Value("${paystack.api.base-url}")
    private String baseUrl;

    public PayStackServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<BankResolverResponse> resolveBankAccount(String accountNumber, String bankCode) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/bank/resolve")
                .queryParam("account_number", accountNumber)
                .queryParam("bank_code", bankCode)
                .toUriString();

        return this.webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(ErrorResponse.class)
                                .flatMap(errorResponse -> Mono.error(new RuntimeException(errorResponse.getMessage())))
                )
                .bodyToMono(String.class)
                .flatMap(response -> {
                    try {
                        return Mono.just(new ObjectMapper().readValue(response, BankResolverResponse.class));
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException(e));
                    }
                });
    }


    @Override
    public Mono<FlutterwaveResponse> getBanks(String country) {
        return null;
    }

    @Override
    public Mono<ExchangeRateResponse> getExchangeRate(ExchangeRateRequest request) {
        return null;
    }

    @Override
    public GenericResponse<Beneficiary> addBeneficiary(AddBeneficiaryRequest beneficiary) {
        return null;
    }

    @Override
    public GenericResponse<WithdrawalData> withdraw(FlutterwaveWithdrawalRequest withdrawalRequest) {
        return null;
    }

    @Override
    public GenericResponse<List<FeeData>> getFees(double amount, String currency) {
        return null;
    }

    @Override
    public ResponseEntity<String> handleWebhook(WebhookPayload payload) {
        return null;
    }

    @Override
    public ResponseEntity<String> handleWebhook(String payload) {
        return null;
    }

    @Override
    public PaymentResponse createPayment(double amount, String email) throws Exception {
        return null;
    }

    @Override
    public GenericResponse<String> deleteBeneficiary(Integer beneficiaryId) {
        return null;
    }

    @Override
    public ApiResponse<String> processRefund(RefundRequest refundRequest) {
        return null;
    }
}
