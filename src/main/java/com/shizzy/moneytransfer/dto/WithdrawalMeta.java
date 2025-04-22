package com.shizzy.moneytransfer.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public  class WithdrawalMeta implements Serializable {
    private static final long serialVersionUID = 1L;
    private String beneficiary_name;
    private String account_number;
    private String routing_number;
    private String swift_code;
    private String bank_name;
    private String beneficiary_country;
    private String postal_code;
    private String street_number;
    private String street_name;
    private String city;
    private String sender;
    private String sender_country;
    private String mobile_number;
    private String merchant_name;
    private String first_name;
    private String last_name;
    private String email;
    private String beneficiary_address;
}
