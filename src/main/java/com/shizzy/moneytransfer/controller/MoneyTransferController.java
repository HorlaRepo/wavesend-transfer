package com.shizzy.moneytransfer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.CreateTransactionRequestBody;
import com.shizzy.moneytransfer.dto.TransactionResponseDTO;
import com.shizzy.moneytransfer.dto.TransferInitiationResponse;
import com.shizzy.moneytransfer.dto.TransferVerificationRequest;
import com.shizzy.moneytransfer.service.MoneyTransferService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("transfers")
@RequiredArgsConstructor
public class MoneyTransferController {
    
    private final MoneyTransferService moneyTransferService;
    
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<TransferInitiationResponse>> initiateTransfer(
            @Valid @RequestBody CreateTransactionRequestBody requestBody,
            Authentication auth) {
        
        return ResponseEntity.ok(moneyTransferService.initiateTransfer(requestBody, auth.getName()));
    }
    
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> verifyAndTransfer(
            @Valid @RequestBody TransferVerificationRequest request,
            Authentication auth) {
        
        return ResponseEntity.ok(moneyTransferService.verifyAndTransfer(request, auth.getName()));
    }
}