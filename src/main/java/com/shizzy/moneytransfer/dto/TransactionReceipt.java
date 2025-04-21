package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.model.Country;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@RequiredArgsConstructor
public class TransactionReceipt{
    String mtcn;
    LocalDate transactionDate;
    Country origin;
    Country destination;
    double amount;
}
