package com.shizzy.moneytransfer.dto;

public record RefundRequest(
        Integer transactionId,
        String paymentId
) {
}
