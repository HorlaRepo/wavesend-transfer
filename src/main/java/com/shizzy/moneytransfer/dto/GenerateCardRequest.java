package com.shizzy.moneytransfer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GenerateCardRequest {
    @NotNull(message = "Card type is required")
    @NotBlank(message = "Card type is required")
    private String cardType;

    @NotNull(message = "Wallet id is required")
    @NotBlank(message = "Wallet id is required")
    private Long walletId;

    @NotNull(message = "User email is required")
    @NotBlank(message = "User email is required")
    @Email(message = "Invalid email")
    private String userEmail;
}
