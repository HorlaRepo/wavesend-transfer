package com.shizzy.moneytransfer.model;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Address {
    @NotBlank(message = "Address line 1 cannot be blank.")
    @NotNull(message = "Address line 1 cannot be null.")
    private String addressLine1;

    @NotBlank(message = "City cannot be blank.")
    @NotNull(message = "City cannot be null.")
    private String city;

    @NotBlank(message = "State cannot be blank.")
    @NotNull(message = "State cannot be null.")
    private String state;

    @NotBlank(message = "Postal code cannot be blank.")
    @NotNull(message = "Postal code cannot be null.")
    private String postalCode;

    @NotBlank(message = "Country cannot be blank.")
    @NotNull(message = "Country cannot be null.")
    private String country;
}
