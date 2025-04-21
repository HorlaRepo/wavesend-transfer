package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.model.BankAccount;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class WithdrawalInfo {
    private double amount;
    private String currency;
    private String narration;
    private BankAccount bankAccount;
}
