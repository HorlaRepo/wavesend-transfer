package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionNotification {
    private Transaction debitTransaction;
    private Transaction creditTransaction;
    private TransferInfo transferInfo;
    private TransactionOperation operation;
}
