package com.shizzy.moneytransfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class UpdateTransactionRequest {
    @NotNull(message = "Status is required")
    @NotBlank(message = "Status is required")
    private String status;
}
