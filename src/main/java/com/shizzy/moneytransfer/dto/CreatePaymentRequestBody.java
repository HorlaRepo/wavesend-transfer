package com.shizzy.moneytransfer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentRequestBody {

    @NotNull(message = "Amount field cannot be null")
    @NotBlank(message = "Amount field cannot be blank")
    @Min(value = 1, message = "Amount must be greater than 0")
    private double amount;

    @NotNull(message = "Email cannot be null")
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Please provide a valid email")
    private String email;
}
