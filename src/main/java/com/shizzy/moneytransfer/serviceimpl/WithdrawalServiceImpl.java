package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.WithdrawalData;
import com.shizzy.moneytransfer.dto.WithdrawalInitiationResponse;
import com.shizzy.moneytransfer.dto.WithdrawalRequestMapper;
import com.shizzy.moneytransfer.dto.WithdrawalVerificationRequest;
import com.shizzy.moneytransfer.exception.InvalidOtpException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.exception.TransactionLimitExceededException;
import com.shizzy.moneytransfer.repository.WalletRepository;
import com.shizzy.moneytransfer.service.payment.PaymentGatewayStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.shizzy.moneytransfer.service.AccountLimitService;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.OtpService;
import com.shizzy.moneytransfer.service.TransactionLimitService;
import com.shizzy.moneytransfer.service.WithdrawalService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalServiceImpl implements WithdrawalService {
    private PaymentGatewayStrategy flutterwaveStrategy;
    // private final PaymentGatewayStrategy stripeStrategy;
    private final OtpService otpService;
    private final KeycloakService keycloakService;
    private final CacheManager cacheManager;
    private final TransactionLimitService transactionLimitService;
    private final AccountLimitService accountLimitService;
    private final WalletRepository walletRepository;

    private static final String WITHDRAWAL_OPERATION = "Withdrawal";
    private static final String PENDING_WITHDRAWALS_CACHE = "pendingWithdrawalsCache";

    @Autowired
    @Qualifier("flutterwaveStrategy")
    private void setPaymentGatewayStrategy(PaymentGatewayStrategy paymentGatewayStrategy) {
        this.flutterwaveStrategy = paymentGatewayStrategy;
    }

    // public WithdrawalServiceImpl(
    // @Qualifier("flutterwaveStrategy") PaymentGatewayStrategy flutterwaveStrategy,
    // @Qualifier("stripeStrategy") PaymentGatewayStrategy stripeStrategy) {
    // this.flutterwaveStrategy = flutterwaveStrategy;
    // this.stripeStrategy = stripeStrategy;
    // }

    @Override
    public GenericResponse<WithdrawalData> withdraw(WithdrawalRequestMapper request) {
        // Validate withdrawal against account limits
        try {
            BigDecimal amount = new BigDecimal(request.getAmount());
            String walletId = request.getWalletId();

            String userId = walletRepository.findWalletByWalletId(walletId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"))
                    .getCreatedBy();

            // Validate against withdrawal limit
            transactionLimitService.validateWithdrawal(userId, amount);

            // Validate against daily limit
            if (accountLimitService.wouldExceedDailyLimit(userId, amount)) {
                throw new TransactionLimitExceededException(
                        "This withdrawal would exceed your daily transaction limit");
            }

            // Record the transaction for daily limit tracking
            accountLimitService.recordTransaction(userId, amount);

            // Using Flutterwave strategy for now
            GenericResponse<WithdrawalData> response = flutterwaveStrategy.processWithdrawal(request);

            return response;
        } catch (TransactionLimitExceededException e) {
            return new GenericResponse<>(
                    "error",
                    e.getMessage(),
                    null);
        }
    }

    @Override
    public GenericResponse<WithdrawalInitiationResponse> initiateWithdrawal(WithdrawalRequestMapper request,
            String userId) {
        // Validate user and request
        var apiResponse = keycloakService.getUserById(userId);
        if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        var user = apiResponse.getData();

        // Pre-validate the withdrawal against account limits
        try {
            BigDecimal amount = new BigDecimal(request.getAmount());

            // Validate against withdrawal limit
            transactionLimitService.validateWithdrawal(userId, amount);

            // Validate against daily limit
            if (accountLimitService.wouldExceedDailyLimit(userId, amount)) {
                throw new TransactionLimitExceededException(
                        "This withdrawal would exceed your daily transaction limit");
            }
        } catch (TransactionLimitExceededException e) {
            return new GenericResponse<>(
                    "error",
                    e.getMessage(),
                    null);
        }

        // Generate unique token for this withdrawal request
        String withdrawalToken = UUID.randomUUID().toString();

        // Store pending withdrawal details
        Cache pendingWithdrawalsCache = cacheManager.getCache(PENDING_WITHDRAWALS_CACHE);
        if (pendingWithdrawalsCache == null) {
            throw new RuntimeException("Withdrawal cache not configured");
        }
        pendingWithdrawalsCache.put(withdrawalToken, request);

        // Prepare details for OTP email
        Map<String, Object> operationDetails = new HashMap<>();
        operationDetails.put("amount", request.getAmount());
        operationDetails.put("withdrawalMethod", request.getWithdrawalInfo().getBankAccount().getBankName());
        operationDetails.put("accountNumber",
                maskAccountNumber(request.getWithdrawalInfo().getBankAccount().getAccountNumber().toString()));

        // Send OTP
        otpService.sendOtp(
                user.getEmail(),
                user.getFirstName(),
                WITHDRAWAL_OPERATION,
                operationDetails);

        return new GenericResponse<WithdrawalInitiationResponse>(
                "success",
                "Withdrawal initiated. Please check your email for verification code.",
                new WithdrawalInitiationResponse(withdrawalToken));
    }

    @Override
    public GenericResponse<WithdrawalData> verifyAndWithdraw(WithdrawalVerificationRequest request, String userId) {
        // Verify user exists
        var apiResponse = keycloakService.getUserById(userId);
        if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        var user = apiResponse.getData();

        // Verify OTP
        Map<String, Object> operationDetails = otpService.verifyOtp(
                user.getEmail(),
                WITHDRAWAL_OPERATION,
                request.getOtp());

        if (operationDetails == null) {
            throw new InvalidOtpException("Invalid or expired verification code");
        }

        // Retrieve pending withdrawal
        Cache pendingWithdrawalsCache = cacheManager.getCache(PENDING_WITHDRAWALS_CACHE);
        if (pendingWithdrawalsCache == null) {
            throw new RuntimeException("Withdrawal cache not configured");
        }

        Cache.ValueWrapper wrapper = pendingWithdrawalsCache.get(request.getWithdrawalToken());
        if (wrapper == null) {
            throw new ResourceNotFoundException("Withdrawal request expired or not found");
        }

        WithdrawalRequestMapper withdrawalRequest = (WithdrawalRequestMapper) wrapper.get();

        // Final validation against limits before processing
        try {
            BigDecimal amount = new BigDecimal(withdrawalRequest.getAmount());

            // Validate against withdrawal and daily limits
            transactionLimitService.validateWithdrawal(userId, amount);
            if (accountLimitService.wouldExceedDailyLimit(userId, amount)) {
                throw new TransactionLimitExceededException(
                        "This withdrawal would exceed your daily transaction limit");
            }
        } catch (TransactionLimitExceededException e) {
            return new GenericResponse<>(
                    "error",
                    e.getMessage(),
                    null);
        }

        // Clean up cache
        pendingWithdrawalsCache.evict(request.getWithdrawalToken());

        // Execute the actual withdrawal
        return withdraw(withdrawalRequest);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }

        int visibleDigits = 4;
        int length = accountNumber.length();
        String lastFour = accountNumber.substring(length - visibleDigits);

        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < length - visibleDigits; i++) {
            masked.append('*');
        }

        return masked.append(lastFour).toString();
    }

}
