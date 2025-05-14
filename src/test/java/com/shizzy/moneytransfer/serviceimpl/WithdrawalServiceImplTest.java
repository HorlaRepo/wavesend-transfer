package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.WithdrawalData;
import com.shizzy.moneytransfer.dto.WithdrawalRequestMapper;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.exception.TransactionLimitExceededException;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.AccountLimitService;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.OtpService;
import com.shizzy.moneytransfer.service.TransactionLimitService;
import com.shizzy.moneytransfer.service.payment.PaymentGatewayStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceImplTest {

    @Mock
    private PaymentGatewayStrategy flutterwaveStrategy;

    @Mock
    private OtpService otpService;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private TransactionLimitService transactionLimitService;

    @Mock
    private AccountLimitService accountLimitService;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WithdrawalServiceImpl withdrawalService;

    private WithdrawalRequestMapper requestMapper;
    private Wallet wallet;
    private String userId;
    private String walletId;
    private String amount;
    private BigDecimal amountBD;
    private WithdrawalData withdrawalData;

    @BeforeEach
    void setUp() {
        userId = "user123";
        walletId = "wallet123";
        amount = "1000.00";
        amountBD = new BigDecimal(amount);

        wallet = new Wallet();
        wallet.setWalletId(walletId);
        wallet.setCreatedBy(userId);

        requestMapper = new WithdrawalRequestMapper();
        requestMapper.setAmount(Double.valueOf(amount));
        requestMapper.setWalletId(walletId);

        withdrawalData = new WithdrawalData();
        withdrawalData.setId(10);

        ReflectionTestUtils.setField(withdrawalService, "flutterwaveStrategy", flutterwaveStrategy);

    }

    @Test
    void withdraw_SuccessfulWithdrawal_ReturnsSuccessResponse() {
        // Arrange
        when(walletRepository.findWalletByWalletId(walletId)).thenReturn(Optional.of(wallet));
        // Stubbing
        doNothing().when(transactionLimitService)
                .validateWithdrawal(eq(userId), any(BigDecimal.class));
        when(accountLimitService.wouldExceedDailyLimit(eq(userId), any())).thenReturn(false);
        GenericResponse<WithdrawalData> expectedResponse = new GenericResponse<>("success", "Withdrawal successful",
                withdrawalData);
        when(flutterwaveStrategy.processWithdrawal(requestMapper)).thenReturn(expectedResponse);

        // Act
        GenericResponse<WithdrawalData> response = withdrawalService.withdraw(requestMapper);

        // Assert
        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals(withdrawalData, response.getData());

        // Verification
        verify(transactionLimitService).validateWithdrawal(eq(userId), any(BigDecimal.class));
        verify(accountLimitService).wouldExceedDailyLimit(eq(userId), any(BigDecimal.class));
        verify(accountLimitService).recordTransaction(eq(userId), any(BigDecimal.class));
    }

    @Test
    void withdraw_WalletNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(walletRepository.findWalletByWalletId(walletId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> withdrawalService.withdraw(requestMapper));

        verify(walletRepository).findWalletByWalletId(walletId);
        verify(transactionLimitService, never()).validateWithdrawal(anyString(), any(BigDecimal.class));
        verify(flutterwaveStrategy, never()).processWithdrawal(any());
    }

    @Test
    void withdraw_WithdrawalLimitExceeded_ReturnsErrorResponse() {
        // Arrange
        when(walletRepository.findWalletByWalletId(walletId)).thenReturn(Optional.of(wallet));
        doThrow(new TransactionLimitExceededException("Withdrawal limit exceeded"))
                .when(transactionLimitService)
                .validateWithdrawal(eq(userId), any(BigDecimal.class));
        // Act
        GenericResponse<WithdrawalData> response = withdrawalService.withdraw(requestMapper);

        // Assert
        assertNotNull(response);
        assertEquals("error", response.getStatus());
        assertEquals("Withdrawal limit exceeded", response.getMessage());
        assertNull(response.getData());

        verify(accountLimitService, never()).recordTransaction(anyString(), any());
        verify(flutterwaveStrategy, never()).processWithdrawal(any());
    }

    @Test
    void withdraw_DailyLimitExceeded_ReturnsErrorResponse() {
        // Arrange
        when(walletRepository.findWalletByWalletId(walletId)).thenReturn(Optional.of(wallet));
        doNothing().when(transactionLimitService).validateWithdrawal(eq(userId), any(BigDecimal.class));
        when(accountLimitService.wouldExceedDailyLimit(eq(userId), any(BigDecimal.class))).thenReturn(true);

        // Act
        GenericResponse<WithdrawalData> response = withdrawalService.withdraw(requestMapper);

        // Assert
        assertNotNull(response);
        assertEquals("error", response.getStatus());
        assertEquals("This withdrawal would exceed your daily transaction limit", response.getMessage());
        assertNull(response.getData());

        verify(accountLimitService, never()).recordTransaction(anyString(), any());
        verify(flutterwaveStrategy, never()).processWithdrawal(any());
    }
}