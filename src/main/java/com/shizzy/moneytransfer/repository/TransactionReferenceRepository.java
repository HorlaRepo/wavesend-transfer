package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.TransactionReference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionReferenceRepository extends JpaRepository<TransactionReference, Long> {

    boolean existsByReferenceNumber(String referenceNumber);

    Optional<TransactionReference> findByReferenceNumber(String referenceNumber);
}
