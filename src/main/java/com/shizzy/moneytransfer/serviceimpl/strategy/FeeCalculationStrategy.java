package com.shizzy.moneytransfer.serviceimpl.strategy;

import java.math.BigDecimal;

public interface FeeCalculationStrategy {
    BigDecimal calculateFee(BigDecimal amount);
}
