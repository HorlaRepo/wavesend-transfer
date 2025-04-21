package com.shizzy.moneytransfer.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ExchangeRateRequest {
    private double amount;
    private String destinationCurrency;
    private String sourceCurrency;
}
