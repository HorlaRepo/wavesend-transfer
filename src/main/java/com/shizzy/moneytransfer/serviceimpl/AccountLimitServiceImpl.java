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
import com.shizzy.moneytransfer.service.AccountLimitService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLimitServiceImpl implements AccountLimitService {

    private final AccountLimitRepository accountLimitRepository;
    private final DailyTransactionTotalRepository dailyTransactionTotalRepository;
    private final KycVerificationRepository kycVerificationRepository;
    private final UserAccountLimitRepository userAccountLimitRepository;

    /**
     * Get the verification level of a specific user based on their KYC status
     */
    @Override
    public VerificationLevel getUserVerificationLevel(String userId) {
        // First check if the user has a specific verification level assigned
        Optional<UserAccountLimit> userLimitOpt = userAccountLimitRepository.findByUserId(userId);
        if (userLimitOpt.isPresent()) {
            return userLimitOpt.get().getVerificationLevel();
        }

        // Fall back to KYC-based verification level determination
        Optional<KycVerification> kycOpt = kycVerificationRepository.findByUserId(userId);

        // If no KYC record exists, user is email verified (minimum level)
        if (kycOpt.isEmpty()) {
            return VerificationLevel.EMAIL_VERIFIED;
        }

        KycVerification kyc = kycOpt.get();

        // If both ID and address are verified, user is fully verified
        if (kyc.getIdVerificationStatus() == VerificationStatus.APPROVED &&
                kyc.getAddressVerificationStatus() == VerificationStatus.APPROVED) {
            return VerificationLevel.FULLY_VERIFIED;
        }

        // If only ID is verified, user has ID verification
        if (kyc.getIdVerificationStatus() == VerificationStatus.APPROVED) {
            return VerificationLevel.ID_VERIFIED;
        }

        // Otherwise, user only has email verification
        return VerificationLevel.EMAIL_VERIFIED;
    }

    /**
     * Get the account limits for a specific verification level
     */
    @Override
    public AccountLimitDTO getLimitsForLevel(VerificationLevel level) {
        AccountLimit limits = accountLimitRepository.findByVerificationLevel(level)
                .orElseThrow(() -> new ResourceNotFoundException("Account limits not found for level: " + level));

        return convertToDTO(limits);
    }

    /**
     * Get the account limits for a specific user
     */
    @Override
    public AccountLimitDTO getUserLimits(String userId) {
        VerificationLevel level = getUserVerificationLevel(userId);
        return getLimitsForLevel(level);
    }

    /**
     * Check if a transaction would exceed the daily transaction limit for a user
     */
    @Override
    public boolean wouldExceedDailyLimit(String userId, BigDecimal amount) {
        VerificationLevel level = getUserVerificationLevel(userId);
        AccountLimit limits = accountLimitRepository.findByVerificationLevel(level)
                .orElseThrow(() -> new ResourceNotFoundException("Account limits not found for level: " + level));

        // Fully verified accounts have unlimited transaction capabilities
        if (level == VerificationLevel.FULLY_VERIFIED) {
            return false;
        }

        // Get today's transaction total
        Optional<DailyTransactionTotal> dailyTotalOpt = dailyTransactionTotalRepository
                .findByUserIdAndDate(userId, LocalDate.now());

        BigDecimal currentDailyTotal = dailyTotalOpt
                .map(DailyTransactionTotal::getTotalAmount)
                .orElse(BigDecimal.ZERO);

        // Check if new transaction would exceed daily limit
        BigDecimal newTotal = currentDailyTotal.add(amount);
        return limits.isTransactionExceedingLimit(newTotal);
    }

    /**
     * Check if a new balance would exceed the max balance limit for a user
     */
    @Override
    public boolean wouldExceedBalanceLimit(String userId, BigDecimal newBalance) {
        VerificationLevel level = getUserVerificationLevel(userId);

        // Fully verified accounts have unlimited balance capabilities
        if (level == VerificationLevel.FULLY_VERIFIED) {
            return false;
        }

        AccountLimit limits = accountLimitRepository.findByVerificationLevel(level)
                .orElseThrow(() -> new ResourceNotFoundException("Account limits not found for level: " + level));

        return limits.isBalanceExceedingLimit(newBalance);
    }

    /**
     * Check if a deposit would exceed the deposit limit for a user
     */
    @Override
    public boolean wouldExceedDepositLimit(String userId, BigDecimal amount) {
        VerificationLevel level = getUserVerificationLevel(userId);

        // Fully verified accounts have unlimited deposit capabilities
        if (level == VerificationLevel.FULLY_VERIFIED) {
            return false;
        }

        AccountLimit limits = accountLimitRepository.findByVerificationLevel(level)
                .orElseThrow(() -> new ResourceNotFoundException("Account limits not found for level: " + level));

        return limits.isDepositExceedingLimit(amount);
    }

    /**
     * Check if a withdrawal would exceed the withdrawal limit for a user
     */
    @Override
    public boolean wouldExceedWithdrawalLimit(String userId, BigDecimal amount) {
        VerificationLevel level = getUserVerificationLevel(userId);

        // Fully verified accounts have unlimited withdrawal capabilities
        if (level == VerificationLevel.FULLY_VERIFIED) {
            return false;
        }

        AccountLimit limits = accountLimitRepository.findByVerificationLevel(level)
                .orElseThrow(() -> new ResourceNotFoundException("Account limits not found for level: " + level));

        return limits.isWithdrawalExceedingLimit(amount);
    }

    /**
     * Check if a transfer would exceed the transfer limit for a user
     */
    @Override
    public boolean wouldExceedTransferLimit(String userId, BigDecimal amount) {
        VerificationLevel level = getUserVerificationLevel(userId);

        // Fully verified accounts have unlimited transfer capabilities
        if (level == VerificationLevel.FULLY_VERIFIED) {
            return false;
        }

        AccountLimit limits = accountLimitRepository.findByVerificationLevel(level)
                .orElseThrow(() -> new ResourceNotFoundException("Account limits not found for level: " + level));

        return limits.isTransferExceedingLimit(amount);
    }

    /**
     * Record a transaction for daily limit tracking
     */
    @Override
    @Transactional
    public void recordTransaction(String userId, BigDecimal amount) {
        LocalDate today = LocalDate.now();

        DailyTransactionTotal dailyTotal = dailyTransactionTotalRepository
                .findByUserIdAndDate(userId, today)
                .orElse(DailyTransactionTotal.builder()
                        .userId(userId)
                        .date(today)
                        .totalAmount(BigDecimal.ZERO)
                        .build());

        dailyTotal.addTransaction(amount);
        dailyTransactionTotalRepository.save(dailyTotal);
    }

    /**
     * Update account limits for a verification level
     */
    @Override
    @Transactional
    public ApiResponse<AccountLimitDTO> updateAccountLimits(VerificationLevel level, AccountLimitDTO limitsDTO) {
        AccountLimit limits = accountLimitRepository.findByVerificationLevel(level)
                .orElseThrow(() -> new ResourceNotFoundException("Account limits not found for level: " + level));

        // Update limits
        limits.setDailyTransactionLimit(limitsDTO.getDailyTransactionLimit());
        limits.setMaxWalletBalance(limitsDTO.getMaxWalletBalance());
        limits.setMaxDepositAmount(limitsDTO.getMaxDepositAmount());
        limits.setMaxWithdrawalAmount(limitsDTO.getMaxWithdrawalAmount());
        limits.setMaxTransferAmount(limitsDTO.getMaxTransferAmount());

        // Save updated limits
        limits = accountLimitRepository.save(limits);

        return ApiResponse.<AccountLimitDTO>builder()
                .success(true)
                .message("Account limits updated successfully.")
                .data(convertToDTO(limits))
                .build();
    }

    /**
     * Initialize default account limits for all verification levels
     */
    @Override
    @PostConstruct
    public void initializeDefaultLimits() {
        // Check if limits already exist
        // if (accountLimitRepository.count() > 0) {
        //     log.info("Account limits already initialized, skipping...");
        //     return;
        // }

        // log.info("Initializing default account limits...");
        // LocalDateTime now = LocalDateTime.now();

        // // Unverified - Very limited
        // AccountLimit unverifiedLimits = new AccountLimit();
        // unverifiedLimits.setVerificationLevel(VerificationLevel.UNVERIFIED);
        // unverifiedLimits.setDailyTransactionLimit(BigDecimal.valueOf(200));
        // unverifiedLimits.setMaxWalletBalance(BigDecimal.valueOf(500));
        // unverifiedLimits.setMaxDepositAmount(BigDecimal.valueOf(100));
        // unverifiedLimits.setMaxWithdrawalAmount(BigDecimal.valueOf(50));
        // unverifiedLimits.setMaxTransferAmount(BigDecimal.valueOf(100));
        // unverifiedLimits.setCreatedDate(now); // Set the created date
        // accountLimitRepository.save(unverifiedLimits);

        // // Email Verified - Basic limits
        // AccountLimit emailVerifiedLimits = new AccountLimit();
        // emailVerifiedLimits.setVerificationLevel(VerificationLevel.EMAIL_VERIFIED);
        // emailVerifiedLimits.setDailyTransactionLimit(BigDecimal.valueOf(500));
        // emailVerifiedLimits.setMaxWalletBalance(BigDecimal.valueOf(1000));
        // emailVerifiedLimits.setMaxDepositAmount(BigDecimal.valueOf(300));
        // emailVerifiedLimits.setMaxWithdrawalAmount(BigDecimal.valueOf(200));
        // emailVerifiedLimits.setMaxTransferAmount(BigDecimal.valueOf(500));
        // emailVerifiedLimits.setCreatedDate(now); // Set the created date
        // accountLimitRepository.save(emailVerifiedLimits);

        // // ID Verified - Higher limits
        // AccountLimit idVerifiedLimits = new AccountLimit();
        // idVerifiedLimits.setVerificationLevel(VerificationLevel.ID_VERIFIED);
        // idVerifiedLimits.setDailyTransactionLimit(BigDecimal.valueOf(5000));
        // idVerifiedLimits.setMaxWalletBalance(BigDecimal.valueOf(10000));
        // idVerifiedLimits.setMaxDepositAmount(BigDecimal.valueOf(3000));
        // idVerifiedLimits.setMaxWithdrawalAmount(BigDecimal.valueOf(2000));
        // idVerifiedLimits.setMaxTransferAmount(BigDecimal.valueOf(5000));
        // idVerifiedLimits.setCreatedDate(now); // Set the created date
        // accountLimitRepository.save(idVerifiedLimits);

        // // Fully Verified - Unlimited
        // AccountLimit fullyVerifiedLimits = new AccountLimit();
        // fullyVerifiedLimits.setVerificationLevel(VerificationLevel.FULLY_VERIFIED);
        // fullyVerifiedLimits.setDailyTransactionLimit(null); // null represents unlimited
        // fullyVerifiedLimits.setMaxWalletBalance(null);
        // fullyVerifiedLimits.setMaxDepositAmount(null);
        // fullyVerifiedLimits.setMaxWithdrawalAmount(null);
        // fullyVerifiedLimits.setMaxTransferAmount(null);
        // fullyVerifiedLimits.setCreatedDate(now); // Set the created date
        // accountLimitRepository.save(fullyVerifiedLimits);

        // log.info("Default account limits initialized successfully.");
    }

    /**
     * Convert AccountLimit entity to AccountLimitDTO
     */
    private AccountLimitDTO convertToDTO(AccountLimit limit) {
        return AccountLimitDTO.builder()
                .verificationLevel(limit.getVerificationLevel())
                .dailyTransactionLimit(limit.getDailyTransactionLimit())
                .maxWalletBalance(limit.getMaxWalletBalance())
                .maxDepositAmount(limit.getMaxDepositAmount())
                .maxWithdrawalAmount(limit.getMaxWithdrawalAmount())
                .maxTransferAmount(limit.getMaxTransferAmount())
                .build();
    }
}