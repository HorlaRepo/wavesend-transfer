package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.AccountLimitDTO;
import com.shizzy.moneytransfer.enums.VerificationLevel;
import com.shizzy.moneytransfer.service.AccountLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account-limits")
@RequiredArgsConstructor
public class AccountLimitController {

    private final AccountLimitService accountLimitService;
    
    /**
     * Get account limits for the authenticated user
     */
    @GetMapping("/my-limits")
    public ResponseEntity<ApiResponse<AccountLimitDTO>> getMyAccountLimits(Authentication auth) {
        AccountLimitDTO limits = accountLimitService.getUserLimits(auth.getName());
        
        return ResponseEntity.ok(
            ApiResponse.<AccountLimitDTO>builder()
                .success(true)
                .message("Account limits retrieved successfully.")
                .data(limits)
                .build()
        );
    }
    
    /**
     * Get account limits for a specific verification level (admin only)
     */
    @GetMapping("/level/{level}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountLimitDTO>> getLimitsForLevel(
            @PathVariable("level") VerificationLevel level) {
            
        AccountLimitDTO limits = accountLimitService.getLimitsForLevel(level);
        
        return ResponseEntity.ok(
            ApiResponse.<AccountLimitDTO>builder()
                .success(true)
                .message("Account limits for level " + level + " retrieved successfully.")
                .data(limits)
                .build()
        );
    }
    
    /**
     * Update account limits for a specific verification level (admin only)
     */
    @PutMapping("/level/{level}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountLimitDTO>> updateLimitsForLevel(
            @PathVariable("level") VerificationLevel level,
            @RequestBody AccountLimitDTO limitsDTO) {
            
        ApiResponse<AccountLimitDTO> response = accountLimitService.updateAccountLimits(level, limitsDTO);
        return ResponseEntity.ok(response);
    }
}