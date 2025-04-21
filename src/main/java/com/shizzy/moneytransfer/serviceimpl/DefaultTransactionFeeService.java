package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.dto.TransactionFee;
import com.shizzy.moneytransfer.serviceimpl.strategy.FeeCalculationStrategy;
import com.shizzy.moneytransfer.service.TransactionFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DefaultTransactionFeeService implements TransactionFeeService {
    private final FeeCalculationStrategy feeCalculationStrategy;
    @Override
    public TransactionFee calculateFee(double amount) {
        BigDecimal fee = feeCalculationStrategy.calculateFee(BigDecimal.valueOf(amount));
        return new TransactionFee(fee.doubleValue());
    }
}
