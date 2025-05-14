package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.GenerateCardRequest;
import com.shizzy.moneytransfer.model.Card;

import java.util.List;

public interface CardService {
    ApiResponse<Card> generateCard(GenerateCardRequest cardRequest);
    ApiResponse<List<Card>> findCardByWalletId(Long walletId);
    ApiResponse<String> lockCard(Long cardId);
    ApiResponse<String> unlockCard(Long cardId);
    ApiResponse<String> setCardPin(Long cardId, String pin);
    boolean checkPin(Long cardId, String enteredPin);
    ApiResponse<String> deleteCard(Long cardId);
}
