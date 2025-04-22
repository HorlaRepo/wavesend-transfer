package com.shizzy.moneytransfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferVerificationRequest implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    @NotBlank(message = "Transfer token is required")
    private String transferToken;
    
    @NotBlank(message = "Verification code is required")
    @Size(min = 6, max = 6, message = "Verification code must be 6 digits")
    private String otp;
}