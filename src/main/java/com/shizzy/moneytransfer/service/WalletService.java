package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.CreateTransactionRequestBody;
import com.shizzy.moneytransfer.dto.TransactionResponseDTO;
import com.shizzy.moneytransfer.dto.WithdrawalData;
import com.shizzy.moneytransfer.dto.WithdrawalRequestMapper;
import com.shizzy.moneytransfer.model.Wallet;
import com.stripe.model.checkout.Session;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;

public interface WalletService {
    Wallet createWallet(String userId);
    boolean isWalletNew(String walletId);
    Wallet getWalletByWalletId(String walletId);
    void flagWallet(String walletId);
    void unflagWallet(String walletId);
    Wallet findWalletOrThrow(String walletId);
    void verifyWalletBalance(BigDecimal balance, BigDecimal amount);
    ApiResponse<Wallet> getWalletByCreatedBy(Authentication connectedUser);
    BigDecimal getWalletBalance(String walletId);
    Wallet updateWallet(Wallet wallet);
    void deposit(Wallet destinationWallet, BigDecimal amount);
    void transfer(Wallet sourceWallet, Wallet destinationWallet, BigDecimal amount);
    void debit(Wallet sourceWallet, BigDecimal amount);
    void deleteWalletByWalletId(String walletId);
    void deleteWalletByUserId(Long userId);
    boolean existsWalletByWalletId(String walletId);
    boolean existsWalletByUserId(Long userId);
}
