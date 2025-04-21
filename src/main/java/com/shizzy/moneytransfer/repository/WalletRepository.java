package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long>{
    boolean existsWalletByWalletId(String walletId);
    Optional<Wallet> findWalletByWalletId(String walletId);
    Optional<Wallet> findWalletByCreatedBy(String userId);
    boolean existsWalletByCreatedBy(String userId);
}
