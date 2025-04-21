package com.shizzy.moneytransfer.dto;

import jakarta.persistence.MappedSuperclass;
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
public abstract class  BaseWithdrawalRequest {
    private double amount;
    private   String narration;
    private String currency;
    private String reference;
    private String beneficiary_name;
}
