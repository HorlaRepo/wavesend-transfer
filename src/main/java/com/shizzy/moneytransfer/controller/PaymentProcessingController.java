package com.shizzy.moneytransfer.controller;

import com.google.gson.JsonSyntaxException;
import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.service.PaymentProcessingService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("payment")
@RequiredArgsConstructor
@Slf4j

public class PaymentProcessingController {

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${flutterwave.api.secret-hash}")
    private String secretHash;

    private final PaymentProcessingService paymentProcessingService;

    @PostMapping("/withdraw")
    public GenericResponse<WithdrawalData> withdraw(@RequestBody WithdrawalRequestMapper requestMapper) {
        return paymentProcessingService.withdraw(requestMapper);
    }

    @PostMapping("/send")
    ApiResponse<TransactionResponseDTO> sendMoney(@RequestBody CreateTransactionRequestBody requestBody) {
        return paymentProcessingService.sendMoney(requestBody);
    }

    @PostMapping("/deposit")
    ResponseEntity<PaymentResponse> createStripePayment(@RequestBody CreatePaymentRequestBody requestBody)
            throws Exception {

        PaymentResponse paymentResponse = paymentProcessingService.createStripePayment(requestBody.getAmount(),
                requestBody.getEmail());
        return ResponseEntity.ok(paymentResponse);

    }

    @PostMapping("/stripe-webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestHeader("Stripe-Signature") String sigHeader,
            @RequestBody String payload) {
        log.info(payload);
        try {
            Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (JsonSyntaxException e) {
            System.out.println("Invalid payload");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        } catch (SignatureVerificationException e) {
            System.out.println("Invalid signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }
        return paymentProcessingService.handleStripeWebhook(payload);
    }

    @PostMapping("/flutterwave-webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody WebhookPayload payload,
            @RequestHeader("verif-hash") String signature) {
        if (!signature.equals(secretHash)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        return paymentProcessingService.handleFlutterwaveWebhook(payload);
    }

}
