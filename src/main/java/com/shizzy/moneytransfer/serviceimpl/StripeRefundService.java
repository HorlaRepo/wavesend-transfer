package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.exception.PaymentException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class StripeRefundService {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    public Refund createRefund(String paymentIntentId,
                               BigDecimal amount,
                               String depositId,
                               String refundId) throws PaymentException {
        try {
            Stripe.apiKey = stripeApiKey;

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                    .putMetadata("depositId", depositId)
                    .putMetadata("refundId", refundId)
                    .build();

            return Refund.create(params);
        } catch (StripeException e) {
            log.error("Failed to process refund: {}", e.getMessage());
            throw new PaymentException("Failed to process refund: " + e.getMessage());
        }
    }

    public Refund retrieveRefund(String refundId) throws StripeException {
        Stripe.apiKey = stripeApiKey;
        return Refund.retrieve(refundId);
    }
}
