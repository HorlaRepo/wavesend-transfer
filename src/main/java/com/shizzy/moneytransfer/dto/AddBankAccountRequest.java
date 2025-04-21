package com.shizzy.moneytransfer.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.shizzy.moneytransfer.enums.Region;
import com.shizzy.moneytransfer.validators.ValidBankAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ValidBankAccount
public class AddBankAccountRequest {
    private Region region;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "region")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = AfricaBankAccountDto.class, name = "AFRICA"),
            @JsonSubTypes.Type(value = EUBankAccountDto.class, name = "EU"),
            @JsonSubTypes.Type(value = USBankAccountDto.class, name = "US")
    })
    private Object bankAccountDetails;
}
