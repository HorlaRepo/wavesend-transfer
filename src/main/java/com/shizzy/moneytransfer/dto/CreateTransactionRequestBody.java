package com.shizzy.moneytransfer.dto;

import java.math.BigDecimal;

public record CreateTransactionRequestBody(
        String senderEmail,
        String receiverEmail,
        BigDecimal amount,
        String narration
) {
}
