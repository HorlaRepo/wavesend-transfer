package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.validators.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthRequestDTO {

    @NotBlank(message = "Username cannot be empty")
    @NotNull(message = "Username cannot be empty")
    @Email(message = "Please provide a valid email address")
    private String username;

    @ValidPassword(message = "Password cannot be empty")
    private String password;
}