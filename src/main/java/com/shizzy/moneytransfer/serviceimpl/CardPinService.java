package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.Card;
import com.shizzy.moneytransfer.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardPinService {
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ApiResponse<String> setCardPin(Long cardId, String pin) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id " + cardId));

        String encodedPin = passwordEncoder.encode(pin);
        cardRepository.createPin(cardId, encodedPin);

        return ApiResponse.<String>builder()
                .success(true)
                .data("Card pin set successfully")
                .message("Card pin set successfully")
                .build();
    }

    public boolean validatePin(Long cardId, String enteredPin) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id " + cardId));
        return passwordEncoder.matches(enteredPin, card.getPin());
    }
}
