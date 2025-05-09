package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.DuplicateResourceException;
import com.shizzy.moneytransfer.exception.InsufficientBalanceException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Wallet testWallet;
    private String userId;
    private String walletId;

    @BeforeEach
    void setUp() {
        userId = "user123";
        walletId = "2000001";
        
        testWallet = Wallet.builder()
                .walletId(walletId)
                .createdBy(userId)
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    void createWallet_Success() {
        when(walletRepository.existsWalletByCreatedBy(userId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        Wallet result = walletService.createWallet(userId);

        assertNotNull(result);
        assertEquals(userId, result.getCreatedBy());
        verify(walletRepository).existsWalletByCreatedBy(userId);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void createWallet_ThrowsException_WhenWalletExists() {
        when(walletRepository.existsWalletByCreatedBy(userId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> walletService.createWallet(userId));
        verify(walletRepository).existsWalletByCreatedBy(userId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void isWalletNew_ReturnsTrueForNewWallet() {
        Wallet newWallet = Wallet.builder()
                .walletId(walletId)
                .createdBy(userId)
                .createdDate(LocalDateTime.now().minusDays(15))
                .build();
        
        when(walletRepository.findWalletByWalletId(walletId)).thenReturn(Optional.of(newWallet));

        boolean result = walletService.isWalletNew(walletId);

        assertTrue(result);
        verify(walletRepository).findWalletByWalletId(walletId);
    }

    @Test
    void isWalletNew_ReturnsFalseForOldWallet() {
        Wallet oldWallet = Wallet.builder()
                .walletId(walletId)
                .createdBy(userId)
                .createdDate(LocalDateTime.now().minusDays(31))
                .build();
        
        when(walletRepository.findWalletByWalletId(walletId)).thenReturn(Optional.of(oldWallet));

        boolean result = walletService.isWalletNew(walletId);

        assertFalse(result);
        verify(walletRepository).findWalletByWalletId(walletId);
    }

    @Test
    void deposit_AddsAmountToWalletBalance() {
        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal depositAmount = new BigDecimal("500.00");
        BigDecimal expectedBalance = new BigDecimal("1500.00");
        
        testWallet.setBalance(initialBalance);
        
        walletService.deposit(testWallet, depositAmount);
        
        assertEquals(expectedBalance, testWallet.getBalance());
        verify(walletRepository).save(testWallet);
    }

    @Test
    void transfer_Success() {
        BigDecimal transferAmount = new BigDecimal("300.00");
        BigDecimal sourceInitialBalance = new BigDecimal("1000.00");
        BigDecimal destInitialBalance = new BigDecimal("500.00");
        
        Wallet sourceWallet = Wallet.builder()
                .walletId("source123")
                .createdBy("sourceUser")
                .balance(sourceInitialBalance)
                .build();
                
        Wallet destWallet = Wallet.builder()
                .walletId("dest456")
                .createdBy("destUser")
                .balance(destInitialBalance)
                .build();
        
        walletService.transfer(sourceWallet, destWallet, transferAmount);
        
        assertEquals(new BigDecimal("700.00"), sourceWallet.getBalance());
        assertEquals(new BigDecimal("800.00"), destWallet.getBalance());
        verify(walletRepository).save(sourceWallet);
        verify(walletRepository).save(destWallet);
    }

    @Test
    void transfer_ThrowsException_WhenInsufficientBalance() {
        BigDecimal transferAmount = new BigDecimal("1500.00");
        BigDecimal sourceInitialBalance = new BigDecimal("1000.00");
        
        Wallet sourceWallet = Wallet.builder()
                .walletId("source123")
                .createdBy("sourceUser")
                .balance(sourceInitialBalance)
                .build();
                
        Wallet destWallet = Wallet.builder()
                .walletId("dest456")
                .createdBy("destUser")
                .balance(BigDecimal.ZERO)
                .build();
        
        assertThrows(InsufficientBalanceException.class, 
                     () -> walletService.transfer(sourceWallet, destWallet, transferAmount));
        
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void debit_Success() {
        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal debitAmount = new BigDecimal("300.00");
        
        testWallet.setBalance(initialBalance);
        
        walletService.debit(testWallet, debitAmount);
        
        assertEquals(new BigDecimal("700.00"), testWallet.getBalance());
        verify(walletRepository).save(testWallet);
    }

    @Test
    void debit_ThrowsException_WhenInsufficientBalance() {
        BigDecimal initialBalance = new BigDecimal("1000.00");
        BigDecimal debitAmount = new BigDecimal("1500.00");
        
        testWallet.setBalance(initialBalance);
        
        assertThrows(InsufficientBalanceException.class, () -> walletService.debit(testWallet, debitAmount));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void getWalletBalance_Success() {
        when(walletRepository.findWalletByWalletId(walletId)).thenReturn(Optional.of(testWallet));
        
        BigDecimal result = walletService.getWalletBalance(walletId);
        
        assertEquals(testWallet.getBalance(), result);
        verify(walletRepository).findWalletByWalletId(walletId);
    }

    @Test
    void getWalletBalance_ThrowsException_WhenWalletNotFound() {
        when(walletRepository.findWalletByWalletId(walletId)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> walletService.getWalletBalance(walletId));
        verify(walletRepository).findWalletByWalletId(walletId);
    }

    @Test
    void flagWallet_Success() {
        testWallet.setFlagged(false);
        when(walletRepository.findWalletByWalletId(walletId)).thenReturn(Optional.of(testWallet));
        
        walletService.flagWallet(walletId);
        
        assertTrue(testWallet.isFlagged());
        verify(walletRepository).save(testWallet);
    }

    @Test
    void unflagWallet_Success() {
        testWallet.setFlagged(true);
        when(walletRepository.findWalletByWalletId(walletId)).thenReturn(Optional.of(testWallet));
        
        walletService.unflagWallet(walletId);
        
        assertFalse(testWallet.isFlagged());
        verify(walletRepository).save(testWallet);
    }

    @Test
    void getWalletByCreatedBy_Success() {
        when(authentication.getName()).thenReturn(userId);
        when(walletRepository.findWalletByCreatedBy(userId)).thenReturn(Optional.of(testWallet));
        
        ApiResponse<Wallet> response = walletService.getWalletByCreatedBy(authentication);
        
        assertTrue(response.isSuccess());
        assertEquals("Wallet retrieved successfully", response.getMessage());
        assertEquals(testWallet, response.getData());
        verify(walletRepository).findWalletByCreatedBy(userId);
    }

    @Test
    void findWalletOrThrow_Success() {
        when(walletRepository.findWalletByCreatedBy(userId)).thenReturn(Optional.of(testWallet));
        
        Wallet result = walletService.findWalletOrThrow(userId);
        
        assertEquals(testWallet, result);
        verify(walletRepository).findWalletByCreatedBy(userId);
    }

    @Test
    void findWalletOrThrow_ThrowsException_WhenWalletNotFound() {
        when(walletRepository.findWalletByCreatedBy(userId)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> walletService.findWalletOrThrow(userId));
        verify(walletRepository).findWalletByCreatedBy(userId);
    }

    @Test
    void verifyWalletBalance_ThrowsException_WhenInsufficientBalance() {
        BigDecimal balance = new BigDecimal("500.00");
        BigDecimal amount = new BigDecimal("1000.00");
        
        assertThrows(InsufficientBalanceException.class, () -> walletService.verifyWalletBalance(balance, amount));
    }
}