package com.shizzy.moneytransfer.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.CreateTransactionRequestBody;
import com.shizzy.moneytransfer.dto.OtpData;
import com.shizzy.moneytransfer.dto.OtpResendRequest;
import com.shizzy.moneytransfer.dto.PendingScheduledTransfer;
import com.shizzy.moneytransfer.dto.ScheduledTransferRequestDTO;
import com.shizzy.moneytransfer.dto.WithdrawalRequestMapper;
import com.shizzy.moneytransfer.enums.EmailTemplateName;
import com.shizzy.moneytransfer.enums.RecurrenceType;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.exception.UnauthorizedAccessException;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    private EmailService emailService;
    private final CacheManager cacheManager;
    private final KeycloakService keycloakService;
    
    // In-memory cache for OTPs - this will work better than trying to use Spring's cache
    // for clean-up operations
    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();

    private static final int OTP_VALIDITY_MINUTES = 5;
    private static final int OTP_LENGTH = 6;

    // Operation constants
    private static final String TRANSFER_OPERATION = "Money Transfer";
    private static final String WITHDRAWAL_OPERATION = "Withdrawal";
    private static final String SCHEDULE_TRANSFER_OPERATION = "Schedule Transfer";

    // Cache names
    private static final String PENDING_TRANSFERS_CACHE = "pendingTransfersCache";
    private static final String PENDING_WITHDRAWALS_CACHE = "pendingWithdrawalsCache";
    private static final String PENDING_SCHEDULED_TRANSFERS_CACHE = "pendingScheduledTransfersCache";

    @Autowired
    @Qualifier("brevoEmailService")
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Generate and send OTP for a specific operation
     */
    public void sendOtp(String userEmail, String userName, String operation, Map<String, Object> operationDetails) {
        String otp = generateOtp();
        String cacheKey = createCacheKey(userEmail, operation);

        // Store OTP in our in-memory map
        otpStore.put(cacheKey, new OtpData(otp, LocalDateTime.now(), operationDetails));

        // Send OTP via email
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("otp", otp);
        templateData.put("operation", operation);
        templateData.put("validity", OTP_VALIDITY_MINUTES);
        templateData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")));
        templateData.put("current_year", LocalDateTime.now().getYear());

        // Add operation-specific details
        if (operationDetails != null) {
            templateData.putAll(operationDetails);
        }

        try {
            emailService.sendEmail(
                    userEmail,
                    templateData,
                    EmailTemplateName.OTP_VERIFICATION,
                    "Security Code for " + operation);
            log.info("OTP sent successfully to {} for {}", userEmail, operation);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    /**
     * Verify OTP for a specific operation
     * 
     * @return OperationDetails if OTP is valid, null otherwise
     */
    public Map<String, Object> verifyOtp(String userEmail, String operation, String providedOtp) {
        String cacheKey = createCacheKey(userEmail, operation);
        
        // Get OTP data from our in-memory map
        OtpData otpData = otpStore.get(cacheKey);
        
        if (otpData == null) {
            log.warn("No OTP found for user {} and operation {}", userEmail, operation);
            return null; // No OTP found
        }

        if (!otpData.getOtp().equals(providedOtp)) {
            log.warn("Invalid OTP provided for user {} and operation {}", userEmail, operation);
            return null; // Invalid OTP
        }

        // Check if OTP is expired
        if (LocalDateTime.now().isAfter(otpData.getCreatedAt().plusMinutes(OTP_VALIDITY_MINUTES))) {
            otpStore.remove(cacheKey);
            log.warn("OTP expired for user {} and operation {}", userEmail, operation);
            return null;
        }

        // Valid OTP - remove it after successful verification (single use)
        Map<String, Object> operationDetails = otpData.getOperationDetails();
        otpStore.remove(cacheKey);
        log.info("OTP verified successfully for user {} and operation {}", userEmail, operation);
        return operationDetails;
    }

    /**
     * Generate a secure random OTP
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int multiplier = (int) Math.pow(10, OTP_LENGTH - 1);
        int number = multiplier + random.nextInt(9 * multiplier); // 6-digit number
        return String.valueOf(number);
    }

    /**
     * Universal OTP resend based on operation type
     */
    public ApiResponse<Void> resendOtp(OtpResendRequest request, String userId) {
        // Verify user
        ApiResponse<UserRepresentation> userResponse = keycloakService.getUserById(userId);
        if (!userResponse.isSuccess() || userResponse.getData() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        var user = userResponse.getData();

        // Process based on operation type
        String operationType = request.getOperationType().toUpperCase();
        String transactionToken = request.getTransactionToken();

        try {
            switch (operationType) {
                case "TRANSFER":
                    resendTransferOtp(transactionToken, user);
                    break;
                case "WITHDRAWAL":
                    resendWithdrawalOtp(transactionToken, user);
                    break;
                case "SCHEDULED_TRANSFER":
                    resendScheduledTransferOtp(transactionToken, user, userId);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported operation type: " + operationType);
            }

            return ApiResponse.<Void>builder()
                    .success(true)
                    .message("Verification code resent. Please check your email.")
                    .build();

        } catch (Exception e) {
            log.error("Error resending OTP: {}", e.getMessage(), e);
            return ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to resend verification code: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Resend OTP for money transfer operation
     */
    private void resendTransferOtp(String transferToken, UserRepresentation user) {
        // Retrieve pending transfer
        Cache pendingTransfersCache = getCacheOrThrow(PENDING_TRANSFERS_CACHE);
        Cache.ValueWrapper wrapper = pendingTransfersCache.get(transferToken);
        
        if (wrapper == null) {
            throw new ResourceNotFoundException("Transfer request expired or not found");
        }
        
        CreateTransactionRequestBody requestBody = (CreateTransactionRequestBody) wrapper.get();
        
        // Security check - ensure it's the user's own transfer
        if (!user.getEmail().equals(requestBody.senderEmail())) {
            throw new UnauthorizedAccessException("You are not authorized to resend OTP for this transfer");
        }
        
        // Get recipient info
        String recipientName = getReceiverName(requestBody.receiverEmail());
        
        // Clear any existing OTP
        clearExistingOtp(user.getEmail(), TRANSFER_OPERATION);
        
        // Prepare details for OTP email
        Map<String, Object> operationDetails = new HashMap<>();
        operationDetails.put("amount", requestBody.amount().toString());
        operationDetails.put("recipient", recipientName);
        operationDetails.put("recipient_email", requestBody.receiverEmail());
        
        // Send fresh OTP
        sendOtp(user.getEmail(), user.getFirstName(), TRANSFER_OPERATION, operationDetails);
        log.info("Resent OTP for money transfer to {}", user.getEmail());
    }

    /**
     * Resend OTP for withdrawal operation
     */
    private void resendWithdrawalOtp(String withdrawalToken, UserRepresentation user) {
        // Retrieve pending withdrawal
        Cache pendingWithdrawalsCache = getCacheOrThrow(PENDING_WITHDRAWALS_CACHE);
        Cache.ValueWrapper wrapper = pendingWithdrawalsCache.get(withdrawalToken);
        
        if (wrapper == null) {
            throw new ResourceNotFoundException("Withdrawal request expired or not found");
        }
        
        WithdrawalRequestMapper request = (WithdrawalRequestMapper) wrapper.get();
        
        // Clear any existing OTP
        clearExistingOtp(user.getEmail(), WITHDRAWAL_OPERATION);
        
        // Prepare details for OTP email
        Map<String, Object> operationDetails = new HashMap<>();
        operationDetails.put("amount", request.getAmount());
        operationDetails.put("withdrawalMethod", request.getWithdrawalInfo().getBankAccount().getBankName());
        operationDetails.put("accountNumber", maskAccountNumber(
                request.getWithdrawalInfo().getBankAccount().getAccountNumber().toString()));
        
        // Send fresh OTP
        sendOtp(user.getEmail(), user.getFirstName(), WITHDRAWAL_OPERATION, operationDetails);
        log.info("Resent OTP for withdrawal to {}", user.getEmail());
    }

    /**
     * Resend OTP for scheduled transfer operation
     */
    private void resendScheduledTransferOtp(String scheduledTransferToken, UserRepresentation user, String userId) {
        // Retrieve pending scheduled transfer
        Cache pendingTransfersCache = getCacheOrThrow(PENDING_SCHEDULED_TRANSFERS_CACHE);
        Cache.ValueWrapper wrapper = pendingTransfersCache.get(scheduledTransferToken);
        
        if (wrapper == null) {
            throw new ResourceNotFoundException("Scheduled transfer request expired or not found");
        }
        
        PendingScheduledTransfer pendingTransfer = (PendingScheduledTransfer) wrapper.get();
        ScheduledTransferRequestDTO request = pendingTransfer.getRequest();
        
        // Security check - ensure it's the user's own transfer
        if (!pendingTransfer.getUserId().equals(userId) || !user.getEmail().equals(request.senderEmail())) {
            throw new UnauthorizedAccessException("You are not authorized to resend OTP for this transfer");
        }
        
        // Get recipient name
        String recipientName = getReceiverName(request.receiverEmail());
        
        // Clear any existing OTP
        clearExistingOtp(user.getEmail(), SCHEDULE_TRANSFER_OPERATION);
        
        // Prepare details for OTP email
        Map<String, Object> operationDetails = new HashMap<>();
        operationDetails.put("amount", request.amount().toString());
        operationDetails.put("recipient", recipientName);
        operationDetails.put("recipient_email", request.receiverEmail());
        operationDetails.put("scheduled_date", request.scheduledDateTime().format(
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        
        if (request.recurrenceType() != null && request.recurrenceType() != RecurrenceType.NONE) {
            operationDetails.put("is_recurring", true);
            operationDetails.put("recurrence_type", request.recurrenceType().toString());
        }
        
        // Send fresh OTP
        sendOtp(user.getEmail(), user.getFirstName(), SCHEDULE_TRANSFER_OPERATION, operationDetails);
        log.info("Resent OTP for scheduled transfer to {}", user.getEmail());
    }

    /**
     * Create a unique cache key for user and operation
     */
    private String createCacheKey(String userEmail, String operation) {
        // Normalize email and operation to avoid case sensitivity issues
        String normalizedEmail = userEmail.toLowerCase().trim();
        String normalizedOperation = operation.trim();
        
        return normalizedEmail + ":" + normalizedOperation;
    }
    
    /**
     * Clear all expired OTPs from the cache
     * This method iterates through all entries and removes those that have expired
     */
    public void clearExpiredOtps() {
        // No more Redis, we can directly iterate through our in-memory map
        int count = 0;
        
        for (Map.Entry<String, OtpData> entry : otpStore.entrySet()) {
            OtpData otpData = entry.getValue();
            
            // Check if OTP is expired
            if (LocalDateTime.now().isAfter(otpData.getCreatedAt().plusMinutes(OTP_VALIDITY_MINUTES))) {
                otpStore.remove(entry.getKey());
                count++;
            }
        }
        
        if (count > 0) {
            log.info("Cleared {} expired OTP entries from in-memory cache", count);
        } else {
            log.debug("No expired OTP entries found in cache");
        }
    }

    /**
     * Clear existing OTP for a user and operation
     */
    private void clearExistingOtp(String userEmail, String operation) {
        String cacheKey = createCacheKey(userEmail, operation);
        otpStore.remove(cacheKey);
        log.debug("Cleared existing OTP for user {} and operation {}", userEmail, operation);
    }

    /**
     * Helper method to safely get a cache or throw exception
     */
    private Cache getCacheOrThrow(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new RuntimeException(cacheName + " not configured");
        }
        return cache;
    }

    /**
     * Helper to get recipient name
     */
    private String getReceiverName(String email) {
        try {
            UserRepresentation user = keycloakService.getUserByEmail(email);
            if (user != null) {
                return user.getFirstName() + " " + user.getLastName();
            } else {
                return email.split("@")[0]; 
            }
        } catch (Exception e) {
            log.warn("Error getting receiver name: {}", e.getMessage());
            return email.split("@")[0];
        }
    }

    /**
     * Helper to mask account number for security
     */
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