package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.model.Transaction;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.function.Function;

@Service
public class TrackTransactionDTOMapper implements Function<Transaction, TrackTransactionDTO> {

    @Override
    public TrackTransactionDTO apply(Transaction transaction) {
        return TrackTransactionDTO.builder()
                .transactionId(transaction.getTransactionId())
                .transactionDate(LocalDate.from(transaction.getTransactionDate()))
                .build();
    }
}
