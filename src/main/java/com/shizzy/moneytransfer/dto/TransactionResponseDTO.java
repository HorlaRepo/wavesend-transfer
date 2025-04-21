package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TransactionResponseDTO(
        String transactionDate,
        BigDecimal amount,
        String status,
        TransactionType transactionType,
        String senderName,
        String senderWalletNumber,
        String receiverName,
        String receiverWalletNumber,
        String referenceNumber
) {
}
