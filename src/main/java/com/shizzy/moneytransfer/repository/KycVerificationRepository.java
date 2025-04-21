package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.KycVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {
    Optional<KycVerification> findByUserId(String userId);
}
