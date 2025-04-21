package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.FlaggedTransactionReason;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlaggedTransactionReasonRepository extends JpaRepository<FlaggedTransactionReason, Long> {
}
