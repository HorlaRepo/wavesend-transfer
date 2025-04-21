package com.shizzy.moneytransfer.dto;

import lombok.Data;

@Data
public class USBankAccountDto {
    private String accountNumber;
    private String accountType;
    private String bankName;
    private String currency;
    private String bankCountry;
    private String routingNumber;
    private String swiftCode;
    private String beneficiaryName;
    private String beneficiaryAddress;
}