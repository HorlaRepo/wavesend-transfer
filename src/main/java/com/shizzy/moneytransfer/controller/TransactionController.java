package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedTransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0", required = false) int page,
            @RequestParam(defaultValue = "50", required = false) int size) {
        ApiResponse<PagedTransactionResponse> response = transactionService.getAllTransactions(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(@PathVariable("id") Integer id) {
        ApiResponse<TransactionResponse> response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wallet/{walletId}/sort")
    public ResponseEntity<ApiResponse<PagedTransactionResponse>> getUserTransactionsBetweenDates(
            @PathVariable String walletId,
            @RequestParam(name = "startDate", required = true) String startDate,
            @RequestParam(name = "endDate", required = true) String endDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "8") int size) {

        TransactionsByDateRequest request = new TransactionsByDateRequest();
        request.setWalletId(walletId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(page);
        request.setSize(size);

        ApiResponse<PagedTransactionResponse> response = transactionService.getUserTransactionsByDate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wallet/{walletId}/filter")
    public ResponseEntity<ApiResponse<PagedTransactionResponse>> getTransactionsByFilter(
            @PathVariable Long walletId,
            @RequestParam(name = "filter", required = true) String filter,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "8") int size) {

        ApiResponse<PagedTransactionResponse> response = transactionService.getTransactionsByFilter(
                walletId, filter, startDate, endDate, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<ApiResponse<PagedTransactionResponse>> getTransactionsByWallet(
            @PathVariable String walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ApiResponse<PagedTransactionResponse> response = transactionService.getTransactionsByWallet(walletId, page,
                size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedTransactionResponse>> searchTransactions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ApiResponse<PagedTransactionResponse> response = transactionService.searchTransactions(query, sortOrder, filter,
                page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Integer id,
            @RequestBody TransactionStatusDTO status) {
        ApiResponse<TransactionResponse> response = transactionService.updateTransaction(id, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reference/{referenceNumber}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionByReferenceNumber(
            @PathVariable @Valid String referenceNumber) {
        ApiResponse<List<TransactionResponse>> response = transactionService
                .getTransactionByReferenceNumber(referenceNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/status/{referenceNumber}")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransactionStatus(
            @PathVariable String referenceNumber,
            @Valid @RequestBody UpdateTransactionRequest request) {
        ApiResponse<TransactionResponse> response = transactionService.updateTransactionStatus(referenceNumber,
                request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transaction-fee")
    public ResponseEntity<ApiResponse<TransactionFee>> getTransactionFee(@RequestParam double amount) {
        ApiResponse<TransactionFee> response = transactionService.getTransactionFee(amount);
        return ResponseEntity.ok(response);
    }

}
