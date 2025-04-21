package com.shizzy.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionsByDateRequest {
    private String walletId;
    private String startDate;
    private String endDate;
    private int page;
    private int size;
}
