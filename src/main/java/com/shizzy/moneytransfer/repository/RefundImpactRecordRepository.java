package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.RefundImpactRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundImpactRecordRepository extends JpaRepository<RefundImpactRecord, Long> {
    List<RefundImpactRecord> findByDepositTransactionId(Integer depositTransactionId);
}