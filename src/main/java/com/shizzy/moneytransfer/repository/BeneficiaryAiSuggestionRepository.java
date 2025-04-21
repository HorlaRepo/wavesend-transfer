package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.BeneficiaryAiSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BeneficiaryAiSuggestionRepository extends JpaRepository<BeneficiaryAiSuggestion, Long> {
    List<BeneficiaryAiSuggestion> findByUserIdAndDismissedFalseAndExpiresAtAfter(
            String userId, LocalDateTime now);
    
    boolean existsByUserIdAndBeneficiaryIdAndExpiresAtAfter(
            String userId, Long beneficiaryId, LocalDateTime now);
    
    void deleteByExpiresAtBefore(LocalDateTime time);
}