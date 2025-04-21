package com.shizzy.moneytransfer.serviceimpl.builder;

import com.shizzy.moneytransfer.dto.EUBankAccountDto;
import com.shizzy.moneytransfer.enums.Region;
import com.shizzy.moneytransfer.model.BankAccount;
import org.springframework.stereotype.Component;

@Component
public class EUBankAccountBuilder implements BankAccountBuilder {
    @Override
    public BankAccount buildBankAccount(Object dto, String createdBy) {
        if (!(dto instanceof EUBankAccountDto details)) {
            throw new IllegalArgumentException("Invalid request type for EU region");
        }

        return BankAccount.builder()
                .region(Region.EU.name().toLowerCase())
                .accountNumber(details.getAccountNumber())
                .accountType(details.getAccountType())
                .bankName(details.getBankName())
                .currency(details.getCurrency())
                .bankCountry(details.getBankCountry())
                .routingNumber(details.getRoutingNumber())
                .swiftCode(details.getSwiftCode())
                .beneficiaryName(details.getBeneficiaryName())
                .beneficiaryAddress(details.getBeneficiaryAddress())
                .beneficiaryCountry(details.getBeneficiaryCountry())
                .postalCode(details.getPostalCode())
                .streetNumber(details.getStreetNumber())
                .streetName(details.getStreetName())
                .city(details.getCity())
                .createdBy(createdBy)
                .build();
    }
}
