package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.GenerateCardRequest;
import com.shizzy.moneytransfer.model.Card;

import java.util.List;

public interface CardService {
    ApiResponse<Card> generateCard(GenerateCardRequest cardRequest);
    ApiResponse<List<Card>> findCardByWalletId(Long walletId);
    ApiResponse<String> lockCard(Integer cardId);
    ApiResponse<String> unlockCard(Integer cardId);
    ApiResponse<String> setCardPin(Integer cardId, String pin);
    boolean checkPin(Integer cardId, String enteredPin);
    ApiResponse<String> deleteCard(Integer cardId);
}
