package com.shizzy.moneytransfer.repository;

import com.shizzy.moneytransfer.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findCardByWalletId(Long walletId);

    @Transactional
    @Modifying
    @Query("UPDATE Card c SET c.pin = :pin, c.isLocked = false  WHERE c.id = :cardId")
    void createPin(@Param("cardId") Long cardId, @Param("pin") String pin);
}
