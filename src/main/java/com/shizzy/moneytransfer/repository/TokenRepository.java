package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);
    void deleteByToken(String token);
    Optional<Token> findByUserIdAndTokenType(UUID userId, com.shizzy.moneytransfer.enums.TokenType tokenType);
}
