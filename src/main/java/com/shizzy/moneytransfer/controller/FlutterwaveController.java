package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.dto.Beneficiary;
import com.shizzy.moneytransfer.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("flutter")
public class FlutterwaveController {

    @Value("${flutterwave.api.secret-hash}")
    private String secretHash;

    private PaymentService paymentService;

    @Autowired
    public void setPaymentService( @Qualifier("flutterwaveService") PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/banks/{country}")
    public Mono<FlutterwaveResponse> getBanks(@PathVariable String country) {
        return paymentService.getBanks(country);
    }

    @GetMapping("/rates")
    public Mono<ExchangeRateResponse> getExchangeRate(@ModelAttribute ExchangeRateRequest request) {
        return paymentService.getExchangeRate(request);
    }

    @PostMapping("/beneficiaries")
    public GenericResponse<Beneficiary> addBeneficiary(@RequestBody AddBeneficiaryRequest beneficiary) {
        return paymentService.addBeneficiary(beneficiary);
    }

    @PostMapping("/withdraw")
    public GenericResponse<WithdrawalData> withdraw(@RequestBody FlutterwaveWithdrawalRequest withdrawalRequest) {
        return paymentService.withdraw(withdrawalRequest);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody WebhookPayload payload, @RequestHeader("verif-hash") String signature) {
        if (!signature.equals(secretHash)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        return paymentService.handleWebhook(payload);
    }

    @GetMapping("/fees")
    public GenericResponse<List<FeeData>> getFees(@RequestParam double amount, @RequestParam String currency) {
        return paymentService.getFees(amount, currency);
    }
}
