package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.TransactionFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultTransactionFilterService implements TransactionFilterService {
    private final TransactionRepository transactionRepository;
    @Override
    public ApiResponse<Page<Transaction>> getTransactionByFilter(
            Long walletId,
            String filter,
            Optional<LocalDate> startDate,
            Optional<LocalDate> endDate,
            int pageNumber,
            int pageSize) {

        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "transactionId"));
        Page<Transaction> transactions;

        if (startDate.isPresent() && endDate.isPresent()) {
            LocalDateTime startDateTime = startDate.get().atStartOfDay();
            LocalDateTime endDateTime = endDate.get().atTime(23, 59, 59);
            transactions = getFilteredTransactionsWithDateRange(walletId, filter, startDateTime, endDateTime, pageRequest);
        } else {
            transactions = getFilteredTransactions(walletId, filter, pageRequest);
        }

        return ApiResponse.<Page<Transaction>>builder()
                .success(true)
                .message(transactions.getTotalElements() + " transactions found")
                .data(transactions)
                .build();
    }

    private Page<Transaction> getFilteredTransactionsWithDateRange(
            Long walletId,
            String filter,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            PageRequest pageRequest) {

        return switch (filter.toUpperCase()) {
            case "PAYMENTS_SENT" ->
                    transactionRepository.findByWalletIdAndOperationAndTransactionTypeAndTransactionDateBetween(
                            walletId, TransactionOperation.TRANSFER, TransactionType.DEBIT, startDateTime, endDateTime, pageRequest);
            case "PAYMENTS_RECEIVED" ->
                    transactionRepository.findByWalletIdAndOperationAndTransactionTypeAndTransactionDateBetween(
                            walletId, TransactionOperation.TRANSFER, TransactionType.CREDIT, startDateTime, endDateTime, pageRequest);
            case "REFUNDS" ->
                    transactionRepository.findByWalletIdAndOperationAndTransactionDateBetween(
                            walletId, TransactionOperation.REVERSAL, startDateTime, endDateTime, pageRequest);
            case "WITHDRAWAL" ->
                    transactionRepository.findByWalletIdAndOperationAndTransactionDateBetween(
                            walletId, TransactionOperation.WITHDRAWAL, startDateTime, endDateTime, pageRequest);
            case "DEPOSIT" ->
                    transactionRepository.findByWalletIdAndOperationAndTransactionDateBetween(
                            walletId, TransactionOperation.DEPOSIT, startDateTime, endDateTime, pageRequest);
            default -> throw new IllegalArgumentException("Invalid filter: " + filter);
        };
    }

    private Page<Transaction> getFilteredTransactions(
            Long walletId,
            String filter,
            PageRequest pageRequest) {

        return switch (filter.toUpperCase()) {
            case "PAYMENTS_SENT" ->
                    transactionRepository.findByWalletIdAndOperationAndTransactionType(
                            walletId, TransactionOperation.TRANSFER, TransactionType.DEBIT, pageRequest);
            case "PAYMENTS_RECEIVED" ->
                    transactionRepository.findByWalletIdAndOperationAndTransactionType(
                            walletId, TransactionOperation.TRANSFER, TransactionType.CREDIT, pageRequest);
            case "REFUNDS" ->
                    transactionRepository.findByWalletIdAndOperation(
                            walletId, TransactionOperation.REVERSAL, pageRequest);
            case "WITHDRAWAL" ->
                    transactionRepository.findByWalletIdAndOperation(
                            walletId, TransactionOperation.WITHDRAWAL, pageRequest);
            case "DEPOSIT" ->
                    transactionRepository.findByWalletIdAndOperation(
                            walletId, TransactionOperation.DEPOSIT, pageRequest);
            default -> throw new IllegalArgumentException("Invalid filter: " + filter);
        };
    }
}
