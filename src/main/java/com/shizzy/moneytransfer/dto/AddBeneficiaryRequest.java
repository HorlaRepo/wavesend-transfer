package com.shizzy.moneytransfer.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddBeneficiaryRequest {
    private String account_number;
    private String account_bank;
    private String beneficiary_name;
}
