package com.shizzy.moneytransfer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.WithdrawalData;
import com.shizzy.moneytransfer.dto.WithdrawalInitiationResponse;
import com.shizzy.moneytransfer.dto.WithdrawalRequestMapper;
import com.shizzy.moneytransfer.dto.WithdrawalVerificationRequest;
import com.shizzy.moneytransfer.service.WithdrawalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("withdrawals")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @PostMapping("/initiate")
    public ResponseEntity<GenericResponse<WithdrawalInitiationResponse>> initiateWithdrawal(
            @Valid @RequestBody WithdrawalRequestMapper request, 
            Authentication auth) {
        
        return ResponseEntity.ok(
            withdrawalService.initiateWithdrawal(request, auth.getName())
        );
    }
    
    @PostMapping("/verify")
    public ResponseEntity<GenericResponse<WithdrawalData>> verifyAndWithdraw(
            @Valid @RequestBody WithdrawalVerificationRequest request, 
            Authentication auth) {
        
        return ResponseEntity.ok(
            withdrawalService.verifyAndWithdraw(request, auth.getName())
        );
    }
    
    // For backward compatibility
    @PostMapping
    public ResponseEntity<GenericResponse<WithdrawalData>> withdraw(
            @Valid @RequestBody WithdrawalRequestMapper request) {
        
        return ResponseEntity.ok(withdrawalService.withdraw(request));
    }
}
