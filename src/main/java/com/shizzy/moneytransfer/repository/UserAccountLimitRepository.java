package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.UserAccountLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountLimitRepository extends JpaRepository<UserAccountLimit, Long> {
    
    Optional<UserAccountLimit> findByUserId(String userId);
    
    boolean existsByUserId(String userId);
}