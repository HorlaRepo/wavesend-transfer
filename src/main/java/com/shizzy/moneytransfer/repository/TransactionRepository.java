package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.enums.RefundStatus;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    Optional<Transaction> findTransactionByMtcn(String mtcn);
    List<Transaction> findTransactionByReferenceNumber(String referenceNumber);
    List<Transaction> findTransactionByWalletId(Long wallet_id);

    Page<Transaction> findAll(Specification<Transaction> specification, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.wallet = :userWallet) " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    Page<Transaction> findTransactionsByWalletIdAndDateRange(Wallet userWallet, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    List<Transaction> findTransactionsByWalletAndTransactionDateBetween(Wallet userWallet, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.wallet = :wallet ")
    Page<Transaction> findTransactionsByWallet(Wallet wallet, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.wallet.walletId = :walletId AND t.operation = :operation AND t.transactionDate > :recentDate")
    List<Transaction> findRecentTransactions(@Param("walletId") String walletId, @Param("operation") TransactionOperation operation, @Param("recentDate") LocalDateTime recentDate);

    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.wallet.walletId = :walletId")
    BigDecimal getAverageTransactionAmount(@Param("walletId") String walletId);

    @Query("SELECT MAX(t.transactionDate) FROM Transaction t WHERE t.wallet.walletId = :walletId")
    LocalDateTime findLastTransactionDate(@Param("walletId") String walletId);

    List<Transaction>findByWalletIdAndOperationAndRefundStatusNot(Long walletId, TransactionOperation transactionOperation, RefundStatus refundStatus);

    Page<Transaction> findByWalletIdAndOperationAndTransactionTypeAndTransactionDateBetween(Long walletId, TransactionOperation operation, TransactionType transactionType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<Transaction> findByWalletIdAndOperationAndTransactionDateBetween(Long walletId, TransactionOperation operation, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<Transaction> findByWalletIdAndOperationAndTransactionType(Long walletId, TransactionOperation operation, TransactionType transactionType, Pageable pageable);
    Page<Transaction> findByWalletIdAndOperation(Long walletId, TransactionOperation operation, Pageable pageable);
}


