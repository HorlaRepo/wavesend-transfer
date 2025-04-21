package com.shizzy.moneytransfer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountryPaymentMethodId implements Serializable {
    private Long country;
    private Long paymentMethod;
}
