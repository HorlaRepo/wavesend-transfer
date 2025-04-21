package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionStatusRepository extends JpaRepository<TransactionStatus, Integer> {
}
