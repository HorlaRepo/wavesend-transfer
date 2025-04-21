package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.enums.VerificationLevel;
import com.shizzy.moneytransfer.model.AccountLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountLimitRepository extends JpaRepository<AccountLimit, Long> {
    Optional<AccountLimit> findByVerificationLevel(VerificationLevel verificationLevel);
}