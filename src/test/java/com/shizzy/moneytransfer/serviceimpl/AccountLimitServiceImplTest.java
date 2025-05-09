package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.AccountLimitDTO;
import com.shizzy.moneytransfer.enums.VerificationLevel;
import com.shizzy.moneytransfer.enums.VerificationStatus;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.AccountLimit;
import com.shizzy.moneytransfer.model.DailyTransactionTotal;
import com.shizzy.moneytransfer.model.KycVerification;
import com.shizzy.moneytransfer.model.UserAccountLimit;
import com.shizzy.moneytransfer.repository.AccountLimitRepository;
import com.shizzy.moneytransfer.repository.DailyTransactionTotalRepository;
import com.shizzy.moneytransfer.repository.KycVerificationRepository;
import com.shizzy.moneytransfer.repository.UserAccountLimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountLimitServiceImplTest {

    @Mock
    private AccountLimitRepository accountLimitRepository;

    @Mock
    private DailyTransactionTotalRepository dailyTransactionTotalRepository;

    @Mock
    private KycVerificationRepository kycVerificationRepository;

    @Mock
    private UserAccountLimitRepository userAccountLimitRepository;

    @InjectMocks
    private AccountLimitServiceImpl accountLimitService;

    private final String userId = "test-user-id";
    private AccountLimit fullyVerifiedLimits;
    private AccountLimit idVerifiedLimits;
    private AccountLimit emailVerifiedLimits;
    private AccountLimit unverifiedLimits;

    @BeforeEach
    void setUp() {
        // Setup test data for account limits
        fullyVerifiedLimits = mock(AccountLimit.class);
        when(fullyVerifiedLimits.getVerificationLevel()).thenReturn(VerificationLevel.FULLY_VERIFIED);
        when(fullyVerifiedLimits.getDailyTransactionLimit()).thenReturn(null); // unlimited
        when(fullyVerifiedLimits.getMaxWalletBalance()).thenReturn(null);
        when(fullyVerifiedLimits.getMaxDepositAmount()).thenReturn(null);
        when(fullyVerifiedLimits.getMaxWithdrawalAmount()).thenReturn(null);
        when(fullyVerifiedLimits.getMaxTransferAmount()).thenReturn(null);

        idVerifiedLimits = mock(AccountLimit.class);
        when(idVerifiedLimits.getVerificationLevel()).thenReturn(VerificationLevel.ID_VERIFIED);
        when(idVerifiedLimits.getDailyTransactionLimit()).thenReturn(BigDecimal.valueOf(5000));
        when(idVerifiedLimits.getMaxWalletBalance()).thenReturn(BigDecimal.valueOf(10000));
        when(idVerifiedLimits.getMaxDepositAmount()).thenReturn(BigDecimal.valueOf(3000));
        when(idVerifiedLimits.getMaxWithdrawalAmount()).thenReturn(BigDecimal.valueOf(2000));
        when(idVerifiedLimits.getMaxTransferAmount()).thenReturn(BigDecimal.valueOf(5000));

        emailVerifiedLimits = new AccountLimit();
        emailVerifiedLimits.setVerificationLevel(VerificationLevel.EMAIL_VERIFIED);
        emailVerifiedLimits.setDailyTransactionLimit(BigDecimal.valueOf(500));
        emailVerifiedLimits.setMaxWalletBalance(BigDecimal.valueOf(1000));
        emailVerifiedLimits.setMaxDepositAmount(BigDecimal.valueOf(300));
        emailVerifiedLimits.setMaxWithdrawalAmount(BigDecimal.valueOf(200));
        emailVerifiedLimits.setMaxTransferAmount(BigDecimal.valueOf(500));

        unverifiedLimits = new AccountLimit();
        unverifiedLimits.setVerificationLevel(VerificationLevel.UNVERIFIED);
        unverifiedLimits.setDailyTransactionLimit(BigDecimal.valueOf(200));
        unverifiedLimits.setMaxWalletBalance(BigDecimal.valueOf(500));
        unverifiedLimits.setMaxDepositAmount(BigDecimal.valueOf(100));
        unverifiedLimits.setMaxWithdrawalAmount(BigDecimal.valueOf(50));
        unverifiedLimits.setMaxTransferAmount(BigDecimal.valueOf(100));
    }

    @Test
    void getUserVerificationLevel_WithUserAccountLimit_ReturnsUserSpecificLevel() {
        UserAccountLimit userLimit = new UserAccountLimit();
        userLimit.setUserId(userId);
        userLimit.setVerificationLevel(VerificationLevel.ID_VERIFIED);
        when(userAccountLimitRepository.findByUserId(userId)).thenReturn(Optional.of(userLimit));

        VerificationLevel level = accountLimitService.getUserVerificationLevel(userId);

        assertEquals(VerificationLevel.ID_VERIFIED, level);
        verify(userAccountLimitRepository).findByUserId(userId);
        verifyNoInteractions(kycVerificationRepository);
    }

    @Test
    void getUserVerificationLevel_WithFullVerification_ReturnsFullyVerified() {
        when(userAccountLimitRepository.findByUserId(userId)).thenReturn(Optional.empty());

        KycVerification kyc = new KycVerification();
        kyc.setUserId(userId);
        kyc.setIdVerificationStatus(VerificationStatus.APPROVED);
        kyc.setAddressVerificationStatus(VerificationStatus.APPROVED);
        when(kycVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(kyc));

        VerificationLevel level = accountLimitService.getUserVerificationLevel(userId);

        assertEquals(VerificationLevel.FULLY_VERIFIED, level);
    }

    @Test
    void getUserVerificationLevel_WithOnlyIdVerification_ReturnsIdVerified() {
        when(userAccountLimitRepository.findByUserId(userId)).thenReturn(Optional.empty());

        KycVerification kyc = new KycVerification();
        kyc.setUserId(userId);
        kyc.setIdVerificationStatus(VerificationStatus.APPROVED);
        kyc.setAddressVerificationStatus(VerificationStatus.PENDING);
        when(kycVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(kyc));

        VerificationLevel level = accountLimitService.getUserVerificationLevel(userId);

        assertEquals(VerificationLevel.ID_VERIFIED, level);
    }

    @Test
    void getUserVerificationLevel_WithNoKyc_ReturnsEmailVerified() {
        when(userAccountLimitRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(kycVerificationRepository.findByUserId(userId)).thenReturn(Optional.empty());

        VerificationLevel level = accountLimitService.getUserVerificationLevel(userId);

        assertEquals(VerificationLevel.EMAIL_VERIFIED, level);
    }

    @Test
    void getLimitsForLevel_WithValidLevel_ReturnsCorrectLimits() {
        when(accountLimitRepository.findByVerificationLevel(VerificationLevel.ID_VERIFIED))
                .thenReturn(Optional.of(idVerifiedLimits));

        AccountLimitDTO result = accountLimitService.getLimitsForLevel(VerificationLevel.ID_VERIFIED);

        assertNotNull(result);
        assertEquals(VerificationLevel.ID_VERIFIED, result.getVerificationLevel());
        assertEquals(idVerifiedLimits.getDailyTransactionLimit(), result.getDailyTransactionLimit());
    }

    @Test
    void getLimitsForLevel_WithInvalidLevel_ThrowsResourceNotFoundException() {
        when(accountLimitRepository.findByVerificationLevel(VerificationLevel.ID_VERIFIED))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountLimitService.getLimitsForLevel(VerificationLevel.ID_VERIFIED));
    }

    @Test
    void wouldExceedDailyLimit_WithFullyVerifiedUser_ReturnsFalse() {
        when(userAccountLimitRepository.findByUserId(userId)).thenReturn(Optional.empty());

        KycVerification kyc = new KycVerification();
        kyc.setUserId(userId);
        kyc.setIdVerificationStatus(VerificationStatus.APPROVED);
        kyc.setAddressVerificationStatus(VerificationStatus.APPROVED);
        when(kycVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(kyc));

        when(accountLimitRepository.findByVerificationLevel(VerificationLevel.FULLY_VERIFIED))
                .thenReturn(Optional.of(fullyVerifiedLimits));

        boolean result = accountLimitService.wouldExceedDailyLimit(userId, BigDecimal.valueOf(10000));

        assertFalse(result);
        verifyNoInteractions(dailyTransactionTotalRepository);
    }

    @Test
    void wouldExceedDailyLimit_WithAmountOverLimit_ReturnsTrue() {
        when(userAccountLimitRepository.findByUserId(userId)).thenReturn(Optional.empty());

        KycVerification kyc = new KycVerification();
        kyc.setUserId(userId);
        kyc.setIdVerificationStatus(VerificationStatus.APPROVED);
        kyc.setAddressVerificationStatus(VerificationStatus.PENDING);
        when(kycVerificationRepository.findByUserId(userId)).thenReturn(Optional.of(kyc));

        when(accountLimitRepository.findByVerificationLevel(VerificationLevel.ID_VERIFIED))
                .thenReturn(Optional.of(idVerifiedLimits));

        DailyTransactionTotal dailyTotal = new DailyTransactionTotal();
        dailyTotal.setUserId(userId);
        dailyTotal.setDate(LocalDate.now());
        dailyTotal.setTotalAmount(BigDecimal.valueOf(3000));
        when(dailyTransactionTotalRepository.findByUserIdAndDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.of(dailyTotal));

        // This will now work because idVerifiedLimits is a mock
        when(idVerifiedLimits.isTransactionExceedingLimit(any(BigDecimal.class))).thenReturn(true);

        boolean result = accountLimitService.wouldExceedDailyLimit(userId, BigDecimal.valueOf(3000));

        assertTrue(result);
    }

    @Test
    void recordTransaction_WithExistingDailyTotal_UpdatesTotal() {
        LocalDate today = LocalDate.now();
        DailyTransactionTotal existingTotal = new DailyTransactionTotal();
        existingTotal.setUserId(userId);
        existingTotal.setDate(today);
        existingTotal.setTotalAmount(BigDecimal.valueOf(500));

        when(dailyTransactionTotalRepository.findByUserIdAndDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.of(existingTotal));

        accountLimitService.recordTransaction(userId, BigDecimal.valueOf(100));

        verify(dailyTransactionTotalRepository).save(any(DailyTransactionTotal.class));
    }

    @Test
    void updateAccountLimits_UpdatesAndReturnsDTOWithSuccess() {
        AccountLimitDTO updatedLimitsDTO = AccountLimitDTO.builder()
                .verificationLevel(VerificationLevel.ID_VERIFIED)
                .dailyTransactionLimit(BigDecimal.valueOf(6000))
                .maxWalletBalance(BigDecimal.valueOf(12000))
                .maxDepositAmount(BigDecimal.valueOf(4000))
                .maxWithdrawalAmount(BigDecimal.valueOf(3000))
                .maxTransferAmount(BigDecimal.valueOf(6000))
                .build();

        when(accountLimitRepository.findByVerificationLevel(VerificationLevel.ID_VERIFIED))
                .thenReturn(Optional.of(idVerifiedLimits));
        when(accountLimitRepository.save(any(AccountLimit.class)))
                .thenReturn(idVerifiedLimits);

        ApiResponse<AccountLimitDTO> response = accountLimitService.updateAccountLimits(
                VerificationLevel.ID_VERIFIED, updatedLimitsDTO);

        assertTrue(response.isSuccess());
        assertEquals("Account limits updated successfully.", response.getMessage());
    }

    @Test
    void initializeDefaultLimits_WithExistingLimits_SkipsInitialization() {
        when(accountLimitRepository.count()).thenReturn(4L);

        accountLimitService.initializeDefaultLimits();

        verify(accountLimitRepository).count();
        verifyNoMoreInteractions(accountLimitRepository);
    }

    @Test
    void initializeDefaultLimits_WithNoExistingLimits_InitializesDefaultLimits() {
        when(accountLimitRepository.count()).thenReturn(0L);
        when(accountLimitRepository.save(any(AccountLimit.class))).thenReturn(new AccountLimit());

        accountLimitService.initializeDefaultLimits();

        verify(accountLimitRepository, times(4)).save(any(AccountLimit.class));
    }
}