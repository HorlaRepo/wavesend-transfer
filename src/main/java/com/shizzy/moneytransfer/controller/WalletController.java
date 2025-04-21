package com.shizzy.moneytransfer.controller;


import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ApiResponse<Wallet> getWalletByUser(Authentication connectedUser) {
        return  walletService.getWalletByCreatedBy(connectedUser);
    }

    @PostMapping
    public ApiResponse<Wallet> createWallet(@RequestBody String userId){
         Wallet wallet = walletService.createWallet(userId);
         return ApiResponse.<Wallet>builder()
                 .success(true)
                 .message("Wallet created successfully")
                 .data(wallet)
                 .build();
    }
}
