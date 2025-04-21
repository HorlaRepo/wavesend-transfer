package com.shizzy.moneytransfer.serviceimpl.builder;

import com.shizzy.moneytransfer.dto.AfricaBankAccountDto;
import com.shizzy.moneytransfer.enums.Region;
import com.shizzy.moneytransfer.model.BankAccount;
import org.springframework.stereotype.Component;

@Component
public class AfricaBankAccountBuilder implements BankAccountBuilder {
    @Override
    public BankAccount buildBankAccount(Object dto, String createdBy) {
        if (!(dto instanceof AfricaBankAccountDto details)) {
            throw new IllegalArgumentException("Invalid data type for Africa region");
        }

        return BankAccount.builder()
                .region(Region.AFRICA.name().toLowerCase())
                .accountNumber(details.getAccountNumber())
                .accountType(details.getAccountType())
                .bankName(details.getBankName())
                .currency(details.getCurrency())
                .bankCountry(details.getBankCountry())
                .accountName(details.getAccountName())
                .bankCode(details.getBankCode())
                .paymentMethod(details.getPaymentMethod())
                .createdBy(createdBy)
                .build();
    }
}
