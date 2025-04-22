package com.shizzy.moneytransfer.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record CreateTransactionRequestBody(
        String senderEmail,
        String receiverEmail,
        BigDecimal amount,
        String narration
) implements Serializable {
        // Adding serialVersionUID is good practice for serializable classes
        private static final long serialVersionUID = 1L;
    }
