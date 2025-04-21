package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.TransactionReference;
import com.shizzy.moneytransfer.serviceimpl.TransactionReferenceServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("transaction-reference")
@RequiredArgsConstructor
public class TransactionReferenceController {

    private final TransactionReferenceServiceImpl transactionReferenceServiceImpl;

    @GetMapping("/{referenceNumber}")
    public ApiResponse<TransactionReference> getTransactionReferenceByReferenceNumber(@PathVariable String referenceNumber) {
        return transactionReferenceServiceImpl.getTransactionReferenceByReferenceNumber(referenceNumber);
    }
}
