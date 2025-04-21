package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.UserBeneficiaryRequest;
import com.shizzy.moneytransfer.dto.UserBeneficiaryResponse;
import com.shizzy.moneytransfer.dto.UserBeneficiariesResponse;
import com.shizzy.moneytransfer.service.UserBeneficiariesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("beneficiaries")
@RequiredArgsConstructor
public class UserBeneficiariesController {

    private final UserBeneficiariesService userBeneficiariesService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<UserBeneficiaryResponse>> addUserBeneficiary(
            @RequestBody @Valid UserBeneficiaryRequest beneficiary, 
            Authentication connectedUser) {
        return ResponseEntity.ok(userBeneficiariesService.addBeneficiary(connectedUser, beneficiary));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserBeneficiariesResponse>> getUserBeneficiaries(
            Authentication connectedUser) {
        return ResponseEntity.ok(userBeneficiariesService.getBeneficiaries(connectedUser));
    }

    @GetMapping("/{beneficiaryId}")
    public ResponseEntity<ApiResponse<UserBeneficiaryResponse>> getUserBeneficiary(
            @PathVariable Long beneficiaryId, 
            Authentication connectedUser) {
        return ResponseEntity.ok(userBeneficiariesService.getBeneficiary(connectedUser, beneficiaryId));
    }

    @DeleteMapping("/{beneficiaryId}")
    public ResponseEntity<ApiResponse<String>> deleteUserBeneficiary(
            @PathVariable Long beneficiaryId, 
            Authentication connectedUser) {
        return ResponseEntity.ok(userBeneficiariesService.deleteBeneficiary(connectedUser, beneficiaryId));
    }
}