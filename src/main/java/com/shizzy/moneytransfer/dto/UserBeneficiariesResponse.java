package com.shizzy.moneytransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBeneficiariesResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<UserBeneficiaryResponse> beneficiaries;
}