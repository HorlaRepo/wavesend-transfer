package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.DailyTransactionTotal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyTransactionTotalRepository extends JpaRepository<DailyTransactionTotal, Long> {
    Optional<DailyTransactionTotal> findByUserIdAndDate(String userId, LocalDate date);
}