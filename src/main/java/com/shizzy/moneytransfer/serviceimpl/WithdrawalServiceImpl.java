package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.WithdrawalData;
import com.shizzy.moneytransfer.dto.WithdrawalInitiationResponse;
import com.shizzy.moneytransfer.dto.WithdrawalRequestMapper;
import com.shizzy.moneytransfer.dto.WithdrawalVerificationRequest;
import com.shizzy.moneytransfer.exception.InvalidOtpException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.service.payment.PaymentGatewayStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.OtpService;
import com.shizzy.moneytransfer.service.WithdrawalService;

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

    private static final String WITHDRAWAL_OPERATION = "Withdrawal";
    private static final String PENDING_WITHDRAWALS_CACHE = "pendingWithdrawalsCache";
    

    @Autowired
    @Qualifier("flutterwaveStrategy")
    private void setPaymentGatewayStrategy(PaymentGatewayStrategy paymentGatewayStrategy) {
        this.flutterwaveStrategy = paymentGatewayStrategy;
    }

    // public WithdrawalServiceImpl(
    //         @Qualifier("flutterwaveStrategy") PaymentGatewayStrategy flutterwaveStrategy,
    //         @Qualifier("stripeStrategy") PaymentGatewayStrategy stripeStrategy) {
    //     this.flutterwaveStrategy = flutterwaveStrategy;
    //     this.stripeStrategy = stripeStrategy;
    // }

    

    @Override
    public GenericResponse<WithdrawalData> withdraw(WithdrawalRequestMapper request) {
        // Using Flutterwave strategy for now, implementation can be changed to use Stripe
        return flutterwaveStrategy.processWithdrawal(request);
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
        
        // // Ensure it's the user's own withdrawal
        // if (!user.getEmail().equals(request.getUserEmail())) {
        //     throw new IllegalArgumentException("You can only withdraw from your own account");
        // }
        
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
        operationDetails.put("accountNumber", maskAccountNumber(request.getWithdrawalInfo().getBankAccount().getAccountNumber().toString()));
        
        // Send OTP
        otpService.sendOtp(
            user.getEmail(),
            user.getFirstName(),
            WITHDRAWAL_OPERATION,
            operationDetails
        );
        
        return new GenericResponse<WithdrawalInitiationResponse>(
            "success",
            "Withdrawal initiated. Please check your email for verification code.",
            new WithdrawalInitiationResponse(withdrawalToken)
        );
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
            request.getOtp()
        );
        
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
