package com.shizzy.moneytransfer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private String paymentUrl;
    private String sessionId;
    private String transactionReference;
}
