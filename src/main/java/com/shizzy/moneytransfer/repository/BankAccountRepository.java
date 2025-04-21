package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Integer> {
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    List<BankAccount> findBankAccountByCreatedBy(String createdBy);
    long countBankAccountByCreatedBy(String createdBy);
}
