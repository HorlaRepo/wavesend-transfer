package com.shizzy.moneytransfer.serviceimpl.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PercentageFeeStrategy implements FeeCalculationStrategy {
    private final BigDecimal percentage;
    private final BigDecimal maxFee;

    public PercentageFeeStrategy() {
        this.percentage = new BigDecimal("0.02");
        this.maxFee = new BigDecimal("100");
    }

    public PercentageFeeStrategy(BigDecimal percentage, BigDecimal maxFee) {
        this.percentage = percentage;
        this.maxFee = maxFee;
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        BigDecimal fee = amount.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
        return fee.compareTo(maxFee) > 0 ? maxFee : fee;
    }
}
