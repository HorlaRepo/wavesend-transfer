package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Integer> {
    Optional<Admin> findAdminByUsername(String username);
    boolean existsAdminByUsername(String username);
    boolean existsAdminByEmail(String email);
}
