package com.shizzy.moneytransfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WithdrawalVerificationRequest {
    @NotBlank(message = "Withdrawal token is required")
    private String withdrawalToken;
    
    @NotBlank(message = "Verification code is required")
    @Size(min = 6, max = 6, message = "Verification code must be 6 digits")
    private String otp;
}
