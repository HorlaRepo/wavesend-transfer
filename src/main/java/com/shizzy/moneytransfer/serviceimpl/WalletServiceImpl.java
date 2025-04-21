package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.DuplicateResourceException;
import com.shizzy.moneytransfer.exception.InsufficientBalanceException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.WalletService;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    private final SecureRandom secureRandom = new SecureRandom();
    private final AtomicInteger prefix = new AtomicInteger(20);

    @Override
    public Wallet createWallet(String userId) {
        if (walletRepository.existsWalletByCreatedBy(userId)) {
            throw new DuplicateResourceException("A wallet with this user ID already exists");
        }
        Wallet wallet = Wallet.builder()
                .walletId(generateWalletId())
                .createdBy(userId)
                .balance(BigDecimal.ZERO)
                .currency("USD")
                .build();
        return walletRepository.save(wallet);
    }

    @Override
    public boolean isWalletNew(String walletId) {
            Wallet wallet = walletRepository.findWalletByWalletId(walletId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

            LocalDateTime thresholdDate = LocalDateTime.now().minusDays(30);
            return wallet.getCreatedDate().isAfter(thresholdDate);
    }

    @Override
    public  void deposit(Wallet destinationWallet, BigDecimal amount) {
        destinationWallet.setBalance(destinationWallet.getBalance().add(amount));
        walletRepository.save(destinationWallet);
    }

    @Override
    @Transactional
    public  void transfer(@NotNull Wallet sourceWallet, Wallet destinationWallet, BigDecimal amount) {
        BigDecimal balance = sourceWallet.getBalance();
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("You do not have sufficient balance to complete this transaction");
        }
        sourceWallet.setBalance(balance.subtract(amount));
        destinationWallet.setBalance(destinationWallet.getBalance().add(amount));
        walletRepository.save(sourceWallet);
        walletRepository.save(destinationWallet);
    }

    @Override
    public void debit(Wallet sourceWallet, BigDecimal amount) {
        if(sourceWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("You do not have sufficient balance to complete this transaction");
        }
        sourceWallet.setBalance(sourceWallet.getBalance().subtract(amount));
        walletRepository.save(sourceWallet);
    }

    private @NotNull String generateWalletId() {
        String walletId;
        do {
            walletId = String.format("%02d%06d", prefix.get(), secureRandom.nextInt(1_000_000));
        } while (walletRepository.existsWalletByWalletId(walletId));

        if (walletId.endsWith("999999")) {
            prefix.incrementAndGet();
        }
        return walletId;
    }

    @Override
    public BigDecimal getWalletBalance(String walletId) {
        Wallet wallet = walletRepository.findWalletByWalletId(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        return wallet.getBalance();
    }

    @Override
    public Wallet getWalletByWalletId(String walletId) {
        return null;
    }

    @Override
    public void flagWallet(String walletId) {
        Wallet wallet = walletRepository.findWalletByWalletId(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        wallet.setFlagged(true);
        walletRepository.save(wallet);
    }

    @Override
    public void unflagWallet(String walletId) {
        Wallet wallet = walletRepository.findWalletByWalletId(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        wallet.setFlagged(false);
        walletRepository.save(wallet);
    }

    @Override
    public ApiResponse<Wallet> getWalletByCreatedBy(Authentication connectedUser) {
        Wallet wallet  =  walletRepository.findWalletByCreatedBy(connectedUser.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        return ApiResponse.<Wallet>builder()
                .success(true)
                .message("Wallet retrieved successfully")
                .data(wallet)
                .build();
    }

    @Override
    public Wallet updateWallet(Wallet wallet) {
        return null;
    }

    @Override
    public void deleteWalletByWalletId(String walletId) {

    }

    @Override
    public Wallet findWalletOrThrow(String userId) {
        return walletRepository.findWalletByCreatedBy(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    public String simulateConcurrentUpdates(Long walletId, BigDecimal amount, int numberOfThreads) throws InterruptedException {

        Wallet wallet = walletRepository.findById(walletId).orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                   debit(wallet, amount);
                } catch (Exception e) {
                    System.out.println("Update failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        Wallet updatedWallet = walletRepository.findById(walletId).orElseThrow(() -> new RuntimeException("Wallet not found"));
        return "Final balance after " + numberOfThreads + " concurrent updates: " + updatedWallet.getBalance();
    }

    @Override
    public void verifyWalletBalance(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("You do not have sufficient balance to complete this transaction");
        }
    }

    @Override
    public void deleteWalletByUserId(Long userId) {

    }

    @Override
    public boolean existsWalletByWalletId(String walletId) {
        return false;
    }

    @Override
    public boolean existsWalletByUserId(Long userId) {
        return false;
    }

}
