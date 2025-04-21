package com.shizzy.moneytransfer.dto;


import com.shizzy.moneytransfer.model.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class TrackTransactionDTO {
    private Integer transactionId;
    private List<TransactionStatus> status;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
}
