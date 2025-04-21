package com.shizzy.moneytransfer.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PinRequest {

    @Size(max = 4, message = "PIN must be a maximum of 4 digits")
    @Size(min = 4, message = "PIN must be a minimum of 4 digits")
    @Pattern(regexp = "\\d+", message = "PIN must be numeric")
    private String pin;
}
