package com.shizzy.moneytransfer.serviceimpl.command;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.service.WalletService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class TransferMoneyCommand implements TransactionCommand<String>{
    private final WalletService walletService;
    private final Wallet senderWallet;
    private final Wallet receiverWallet;
    private final BigDecimal amount;

    @Override
    public ApiResponse<String> execute() {
        walletService.transfer(senderWallet, receiverWallet, amount);
        return ApiResponse.<String>builder()
                .data("Money transferred successfully")
                .success(true)
                .message("Money transferred successfully")
                .build();
    }
}
