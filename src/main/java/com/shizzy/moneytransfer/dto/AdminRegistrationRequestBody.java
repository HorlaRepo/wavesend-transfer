package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.validators.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

@Data
@Builder
public class  AdminRegistrationRequestBody{

    @NotNull(message = "Username cannot be empty")
    @NotBlank(message = "Username cannot be empty")
    private String username;

    @NotNull(message = "First name cannot be empty")
    @NotBlank(message = "First name cannot be empty")
    private String firstName;

    @NotNull(message = "Last name cannot be empty")
    @NotBlank(message = "Last name cannot be empty")
    private String lastName;

    @Email(message = "Please provide a valid email address")
    private String email;

    @NotNull(message = "Password cannot be empty")
    @ValidPassword
    private String password;

    private String dateOfBirth;

}
