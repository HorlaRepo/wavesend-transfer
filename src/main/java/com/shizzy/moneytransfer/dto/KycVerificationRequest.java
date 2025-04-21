package com.shizzy.moneytransfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KycVerificationRequest(
        @NotNull(message = "address document url is required")
        @NotBlank(message = "address document url is required")
        String addressDocumentUrl,

        @NotNull(message = "id document url is required")
        @NotBlank(message = "id document url is required")
        String idDocumentUrl
) {
}
