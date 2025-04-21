package com.shizzy.moneytransfer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "country_payment_method")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(CountryPaymentMethodId.class)
public class CountryPaymentMethod {
    @Id
    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    @Id
    @ManyToOne
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;
}

