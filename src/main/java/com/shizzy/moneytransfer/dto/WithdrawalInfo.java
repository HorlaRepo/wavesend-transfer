package com.shizzy.moneytransfer.dto;

import java.io.Serializable;

import com.shizzy.moneytransfer.model.BankAccount;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class WithdrawalInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private double amount;
    private String currency;
    private String narration;
    private BankAccount bankAccount;
}
