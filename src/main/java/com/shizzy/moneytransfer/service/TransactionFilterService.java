package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.Transaction;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.Optional;

public interface TransactionFilterService {
    ApiResponse<Page<Transaction>> getTransactionByFilter(
            Long walletId,
            String filter,
            Optional<LocalDate> startDate,
            Optional<LocalDate> endDate,
            int pageNumber,
            int pageSize
    );
}
