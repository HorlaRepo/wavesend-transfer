package com.shizzy.moneytransfer.dto;

import com.shizzy.moneytransfer.enums.Gender;
import com.shizzy.moneytransfer.model.Address;
import com.shizzy.moneytransfer.validators.ValidPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRegistrationRequestBody{

    @NotBlank(message = "First name cannot be blank.")
    private String firstName;

    @NotBlank(message = "Last name cannot be blank.")
    private String lastName;

    @Email(message = "Please provide a valid email address.")
    private String email;

    @NotBlank(message = "Password cannot be blank.")
    @ValidPassword
    private String password;

    @NotBlank(message = "Date of birth cannot be blank.")
    private String dateOfBirth;

    @NotBlank(message = "Phone number cannot be blank.")
    private String phoneNumber;

    @NotBlank(message = "Gender cannot be blank.")
    private Gender gender;

    @Valid
    private Address address;
}
