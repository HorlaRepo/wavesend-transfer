package com.shizzy.moneytransfer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.lang.reflect.Field;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlutterwaveWithdrawalRequest {
    private double amount;
    private String currency;
    private String reference;
    private String narration;
    private String account_bank;
    private String account_number;
    private List<WithdrawalMeta> meta;
    private String beneficiary_name;
    private String bank_name;
    private String account_name;
    private String account_type;
    private String bank_country;
    private String bank_code;
    private String region;
    private String swift_code;
    private String routing_number;
    private String beneficiary_address;
    private String beneficiary_country;
    private String postal_code;
    private String street_number;
    private String street_name;
    private String city;
}


