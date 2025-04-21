package com.shizzy.moneytransfer.service.payment;

import com.shizzy.moneytransfer.dto.PaymentResponse;
import com.shizzy.moneytransfer.exception.PaymentException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;


@Service
@Slf4j
@RequiredArgsConstructor
public class StripePaymentProcessor {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;


    /**
     * Creates a Stripe checkout session for wallet deposit
     */
    public Session createCheckoutSession(String email, BigDecimal amount,
                                         String reference, String userId) throws PaymentException {
            try {
                Stripe.apiKey = stripeApiKey;

                SessionCreateParams params = SessionCreateParams.builder()
                        .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .setCustomerEmail(email)
                        .putMetadata("transactionReference", reference)
                        .putMetadata("userName", userId)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("usd")
                                                        .setUnitAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName("Wallet Top Up")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .build();

                RequestOptions options = RequestOptions.builder()
                        .setIdempotencyKey(reference)
                        .build();

                return Session.create(params, options);
            } catch (StripeException e) {
                log.error("Failed to create payment session: {}", e.getMessage());
                throw new PaymentException("Failed to create payment session: " + e.getMessage());
            }

        }


    /**
     * Generates a response from a Stripe session
     */
    public PaymentResponse generatePaymentResponse(Session session, String reference) {
        return PaymentResponse.builder()
                .paymentUrl(session.getUrl())
                .sessionId(session.getId())
                .transactionReference(reference)
                .build();
    }

}
