package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.GenerateCardRequest;
import com.shizzy.moneytransfer.exception.DuplicateResourceException;
import com.shizzy.moneytransfer.exception.IllegalArgumentException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.Card;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.CardRepository;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.serviceimpl.factory.CardGeneratorFactory;
import com.shizzy.moneytransfer.serviceimpl.strategy.CardNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private CardGeneratorFactory cardGeneratorFactory;

    @Mock
    private CardPinService cardPinService;

    @Mock
    private CardNumberGenerator cardNumberGenerator;

    @InjectMocks
    private CardServiceImpl cardService;

    private Wallet wallet;
    private Card card;
    private GenerateCardRequest cardRequest;
    private UserData userData;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        wallet.setId(1L);

        card = Card.builder()
                .cardType("VISA")
                .cardNumber("4111111111111111")
                .expiryDate("12/25")
                .cvv("123")
                .isLocked(true)
                .cardName("John Doe")
                .wallet(wallet)
                .build();

        cardRequest = new GenerateCardRequest();
        cardRequest.setWalletId(1L);
        cardRequest.setCardType("VISA");
        cardRequest.setUserEmail("john.doe@example.com");

        userData = new UserData("John", "Doe");

        lenient().when(cardGeneratorFactory.getGenerator(anyString())).thenReturn(cardNumberGenerator);
        lenient().when(cardNumberGenerator.generateCardNumber()).thenReturn("4111111111111111");
    }

    @Test
    void generateCard_Success() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        // Create UserRepresentation from your userData
        UserRepresentation userRep = new UserRepresentation();
        userRep.setFirstName(userData.getFirstName());
        userRep.setLastName(userData.getLastName());

        // Create the ApiResponse with the UserRepresentation
        ApiResponse<UserRepresentation> apiResponse = ApiResponse.<UserRepresentation>builder()
                .success(true)
                .data(userRep)
                .build();

        // Mock the method
        when(keycloakService.existsUserByEmail(anyString())).thenReturn(apiResponse);

        // Rest of your test remains the same
        when(cardRepository.findCardByWalletId(1L)).thenReturn(new ArrayList<>());
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        ApiResponse<Card> response = cardService.generateCard(cardRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Card generated successfully", response.getMessage());
        assertNotNull(response.getData());

        verify(walletRepository).findById(1L);
        verify(keycloakService).existsUserByEmail(anyString());
        verify(cardRepository).findCardByWalletId(1L);
        verify(cardGeneratorFactory).getGenerator("VISA");
        verify(cardNumberGenerator).generateCardNumber();
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void generateCard_WalletNotFound() {
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cardService.generateCard(cardRequest));
        verify(walletRepository).findById(1L);
        verifyNoInteractions(cardRepository);
    }

    @Test
    void generateCard_CardTypeDuplicate() {
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        // Create UserRepresentation from your userData
        UserRepresentation userRep = new UserRepresentation();
        userRep.setFirstName(userData.getFirstName());
        userRep.setLastName(userData.getLastName());

        // Create the ApiResponse with the UserRepresentation
        ApiResponse<UserRepresentation> apiResponse = ApiResponse.<UserRepresentation>builder()
                .success(true)
                .data(userRep)
                .build();

        when(keycloakService.existsUserByEmail(anyString())).thenReturn(apiResponse);

        List<Card> existingCards = new ArrayList<>();
        existingCards.add(card); // Already has a VISA card
        when(cardRepository.findCardByWalletId(1L)).thenReturn(existingCards);

        assertThrows(DuplicateResourceException.class, () -> cardService.generateCard(cardRequest));
    }

    @Test
    void findCardByWalletId_Success() {
        List<Card> cards = List.of(card);
        when(cardRepository.findCardByWalletId(1L)).thenReturn(cards);

        ApiResponse<List<Card>> response = cardService.findCardByWalletId(1L);

        assertNotNull(response);
        assertEquals("Cards retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        verify(cardRepository).findCardByWalletId(1L);
    }

    @Test
    void lockCard_Success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        ApiResponse<String> response = cardService.lockCard(1L);

        assertTrue(response.isSuccess());
        assertEquals("Card locked successfully", response.getMessage());
        verify(cardRepository).save(card);
        assertTrue(card.isLocked());
    }

    @Test
    void unlockCard_Success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        ApiResponse<String> response = cardService.unlockCard(1L);

        assertTrue(response.isSuccess());
        assertEquals("Card unlocked successfully", response.getMessage());
        assertEquals("Card unlocked successfully", response.getData());
        verify(cardRepository).save(card);
        assertFalse(card.isLocked());
    }

    @Test
    void setCardPin_Success() {
        ApiResponse<String> pinResponse = ApiResponse.<String>builder()
                .success(true)
                .message("PIN set successfully")
                .build();

        when(cardPinService.setCardPin(1L, "1234")).thenReturn(pinResponse);

        ApiResponse<String> response = cardService.setCardPin(1L, "1234");

        assertTrue(response.isSuccess());
        assertEquals("PIN set successfully", response.getMessage());
        verify(cardPinService).setCardPin(1L, "1234");
    }

    @Test
    void checkPin_Valid() {
        when(cardPinService.validatePin(1L, "1234")).thenReturn(true);

        boolean result = cardService.checkPin(1L, "1234");

        assertTrue(result);
        verify(cardPinService).validatePin(1L, "1234");
    }

    @Test
    void checkPin_Invalid() {
        when(cardPinService.validatePin(1L, "1234")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> cardService.checkPin(1L, "1234"));
        verify(cardPinService).validatePin(1L, "1234");
    }

    @Test
    void deleteCard_Success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        ApiResponse<String> response = cardService.deleteCard(1L);

        assertTrue(response.isSuccess());
        assertEquals("Card deleted successfully", response.getMessage());
        verify(cardRepository).delete(card);
    }

    @Test
    void deleteCard_CardNotFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cardService.deleteCard(1L));
    }

    // Helper class for user data
    private static class UserData {
        private final String firstName;
        private final String lastName;

        public UserData(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }
    }
}