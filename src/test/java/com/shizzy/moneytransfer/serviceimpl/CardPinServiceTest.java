package com.shizzy.moneytransfer.serviceimpl;


import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.Card;
import com.shizzy.moneytransfer.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
class CardPinServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CardPinService cardPinService;

    private Card card;
    private final Long CARD_ID = 1L;
    private final String PIN = "1234";
    private final String ENCODED_PIN = "encodedPin123";

    @BeforeEach
    void setUp() {
        card = new Card();
        card.setId(CARD_ID);
    }

    @Test
    void setCardPin_Success() {
        // Arrange
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(passwordEncoder.encode(PIN)).thenReturn(ENCODED_PIN);
        doNothing().when(cardRepository).createPin(CARD_ID, ENCODED_PIN);

        // Act
        ApiResponse<String> response = cardPinService.setCardPin(CARD_ID, PIN);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Card pin set successfully", response.getData());
        assertEquals("Card pin set successfully", response.getMessage());
        
        // Verify
        verify(cardRepository).findById(CARD_ID);
        verify(passwordEncoder).encode(PIN);
        verify(cardRepository).createPin(CARD_ID, ENCODED_PIN);
    }

    @Test
    void setCardPin_CardNotFound() {
        // Arrange
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> cardPinService.setCardPin(CARD_ID, PIN));
        
        // Verify
        verify(cardRepository).findById(CARD_ID);
        verify(passwordEncoder, never()).encode(anyString());
        verify(cardRepository, never()).createPin(anyLong(), anyString());
    }
}