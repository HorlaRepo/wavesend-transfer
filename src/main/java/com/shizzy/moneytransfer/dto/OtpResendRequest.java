package com.shizzy.moneytransfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpResendRequest {
    @NotBlank(message = "Transaction token is required")
    private String transactionToken;
    
    @NotBlank(message = "Operation type is required")
    @Pattern(regexp = "TRANSFER|WITHDRAWAL|SCHEDULED_TRANSFER", 
             message = "Operation type must be one of: TRANSFER, WITHDRAWAL, SCHEDULED_TRANSFER")
    private String operationType;
}