package com.shizzy.moneytransfer.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ExchangeRateResponse {
    private String status;
    private String message;
    private ExchangeData data;
}


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
class ExchangeData {
    private double rate;
    private CurrencyData source;
    private CurrencyData destination;
}

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
class CurrencyData {
    private String currency;
    private double amount;
}
