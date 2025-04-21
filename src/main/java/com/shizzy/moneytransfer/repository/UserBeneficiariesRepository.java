package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.UserBeneficiaries;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBeneficiariesRepository extends JpaRepository<UserBeneficiaries, String> {
    List<UserBeneficiaries> findByUserId(String userId);

    @Query("SELECT ub FROM UserBeneficiaries ub LEFT JOIN FETCH ub.beneficiaries WHERE ub.userId = :userId")
    Optional<UserBeneficiaries> findByIdWithBeneficiaries(String userId);
}
