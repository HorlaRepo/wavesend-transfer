package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.CardService;
import com.shizzy.moneytransfer.dto.GenerateCardRequest;
import com.shizzy.moneytransfer.exception.DuplicateResourceException;
import com.shizzy.moneytransfer.exception.IllegalArgumentException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.Card;
import com.shizzy.moneytransfer.repository.CardRepository;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.serviceimpl.factory.CardGeneratorFactory;
import com.shizzy.moneytransfer.serviceimpl.strategy.CardNumberGenerator;
import com.shizzy.moneytransfer.util.CardValidationUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;
    private final KeycloakService keycloakService;
    private final CardGeneratorFactory cardGeneratorFactory;
    private final CardPinService cardPinService;

    Random random = new Random();

    @Override
    public ApiResponse<Card> generateCard(GenerateCardRequest cardRequest) {
        // Validate wallet
        Wallet wallet = walletRepository.findById(cardRequest.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id " + cardRequest.getWalletId()));

        // Get user details
        var userData = keycloakService.existsUserByEmail(cardRequest.getUserEmail()).getData();
        String userFirstName = userData.getFirstName();
        String userLastName = userData.getLastName();

        // Validate card constraints
        validateCardConstraints(cardRequest.getWalletId(), cardRequest.getCardType());

        // Generate card details
        CardNumberGenerator generator = cardGeneratorFactory.getGenerator(cardRequest.getCardType());

        // Create and save card
        Card card = Card.builder()
                .cardType(cardRequest.getCardType())
                .cardNumber(generator.generateCardNumber())
                .expiryDate(CardValidationUtils.generateExpiryDate())
                .cvv(CardValidationUtils.generateCVV(random))
                .isLocked(true)
                .cardName(userFirstName + " " + userLastName)
                .wallet(wallet)
                .build();

        cardRepository.save(card);

        return ApiResponse.<Card>builder()
                .data(card)
                .message("Card generated successfully")
                .success(true)
                .build();
    }

    @Override
    public ApiResponse<List<Card>> findCardByWalletId(Long walletId) {
        List<Card> cards =  cardRepository.findCardByWalletId(walletId);

        return ApiResponse.<List<Card>>builder()
                .data(cards)
                .message("Cards retrieved successfully")
                .build();
    }

    @Override
    public ApiResponse<String> lockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id " + cardId));
        card.setLocked(true);
        cardRepository.save(card);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Card locked successfully")
                .build();
    }

    @Override
    public ApiResponse<String> unlockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id " + cardId));
        card.setLocked(false);
        cardRepository.save(card);

        return ApiResponse.<String>builder()
                .success(true)
                .data("Card unlocked successfully")
                .message("Card unlocked successfully")
                .build();
    }


    @Override
    public ApiResponse<String> setCardPin(Long cardId, String pin) {
        return cardPinService.setCardPin(cardId, pin);
    }

    @Override
    public boolean checkPin(Long cardId, String enteredPin) {
        boolean isValid = cardPinService.validatePin(cardId, enteredPin);
        if (!isValid) {
            throw new IllegalArgumentException("Invalid PIN");
        }
        return true;
    }


    @Override
    public ApiResponse<String> deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId).orElseThrow(()-> new ResourceNotFoundException("Card not found"));
        cardRepository.delete(card);

        return ApiResponse.<String>builder()
                .data("Card deleted successfully")
                .message("Card deleted successfully")
                .success(true)
                .build();
    }


    private void validateCardConstraints(Long walletId, String requestedCardType) {
        List<Card> existingCards = cardRepository.findCardByWalletId(walletId);

        // Check for duplicate card types
        for (Card card : existingCards) {
            if (card.getCardType().equalsIgnoreCase(requestedCardType)) {
                throw new DuplicateResourceException("User already has a " + requestedCardType + " card");
            }
        }

        // Check maximum card limit
        if (existingCards.size() >= 2) {
            throw new IllegalArgumentException("You can only have a maximum of 2 cards");
        }
    }

}
