//package com.shizzy.moneytransfer.repository;
//
//import com.shizzy.moneytransfer.model.Token;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.Optional;
//
//
//public interface TokenRepository extends JpaRepository<Token, Long> {
//    Optional<Token> findByToken(String token);
//    void deleteByToken(String token);
//}
