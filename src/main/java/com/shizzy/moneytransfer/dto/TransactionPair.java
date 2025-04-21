package com.shizzy.moneytransfer.dto;


import com.shizzy.moneytransfer.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionPair {
    private final Transaction debitTransaction;
    private final Transaction creditTransaction;
}