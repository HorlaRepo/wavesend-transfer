package com.shizzy.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class AfricaBaseWithdrawalRequest extends BaseWithdrawalRequest {
    private String account_bank;
    private String account_number;
    private String debit_currency;
}
