package com.shizzy.moneytransfer.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shizzy.moneytransfer.enums.RefundStatus;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse implements Serializable{
    
    private static final long serialVersionUID = 1L;


    private Integer transactionId;
    private String providerId;
    private BigDecimal amount;
    private String currentStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private String transactionDate;

    private String referenceNumber;
    private String description;
    private String narration;
    private double fee;
    private TransactionOperation operation;
    private TransactionType transactionType;
    private String sessionId;
    private boolean flagged;
    private RefundStatus refundStatus;

    private String walletId;

}
