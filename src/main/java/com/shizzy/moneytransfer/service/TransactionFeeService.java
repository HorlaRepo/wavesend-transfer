package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.dto.TransactionFee;

public interface TransactionFeeService {
    TransactionFee calculateFee(double amount);
}
