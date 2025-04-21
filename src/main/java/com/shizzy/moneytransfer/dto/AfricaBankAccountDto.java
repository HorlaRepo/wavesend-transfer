package com.shizzy.moneytransfer.dto;

import lombok.Data;

@Data
public class AfricaBankAccountDto {
    private String accountNumber;
    private String accountType;
    private String bankName;
    private String currency;
    private String bankCountry;
    private String accountName;
    private String bankCode;
    private String paymentMethod;
}
