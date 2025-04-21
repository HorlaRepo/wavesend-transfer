package com.shizzy.moneytransfer.serviceimpl.builder;

import com.shizzy.moneytransfer.dto.USBankAccountDto;
import com.shizzy.moneytransfer.enums.Region;
import com.shizzy.moneytransfer.model.BankAccount;
import org.springframework.stereotype.Component;

@Component
public class USBankAccountBuilder implements BankAccountBuilder {
    @Override
    public BankAccount buildBankAccount(Object dto, String createdBy) {
        if (!(dto instanceof USBankAccountDto)) {
            throw new IllegalArgumentException("Invalid DTO type for US region");
        }
        USBankAccountDto details = (USBankAccountDto) dto;

        return BankAccount.builder()
                .region(Region.US.name().toLowerCase())
                .accountNumber(details.getAccountNumber())
                .accountType(details.getAccountType())
                .bankName(details.getBankName())
                .currency(details.getCurrency())
                .bankCountry(details.getBankCountry())
                .routingNumber(details.getRoutingNumber())
                .swiftCode(details.getSwiftCode())
                .beneficiaryName(details.getBeneficiaryName())
                .beneficiaryAddress(details.getBeneficiaryAddress())
                .createdBy(createdBy)
                .build();
    }
}
