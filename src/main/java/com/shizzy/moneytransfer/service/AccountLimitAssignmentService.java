package com.shizzy.moneytransfer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shizzy.moneytransfer.enums.VerificationLevel;
import com.shizzy.moneytransfer.model.UserAccountLimit;
import com.shizzy.moneytransfer.repository.UserAccountLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLimitAssignmentService {

    private final UserAccountLimitRepository userAccountLimitRepository;
    private final AccountLimitService accountLimitService;
    private final ObjectMapper objectMapper;

    /**
     * Assign default account limits to a new user from userId
     * 
     * @param userId The user ID to assign default limits to
     */
    public void assignDefaultLimits(String userId) {
        try {
            // Check if this is a JSON event or direct userId
            if (userId.startsWith("{")) {
                // It's a JSON event, parse it
                handleJsonEvent(userId);
            } else {
                // It's a direct userId, assign limits directly
                assignLimitsToUser(userId);
            }
        } catch (Exception e) {
            log.error("Error assigning default account limits: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Assign limits to a user directly by ID
     * 
     * @param userId The user ID to assign limits to
     */
    private void assignLimitsToUser(String userId) {
        try {
            // Check if user already has limits assigned
            if (userAccountLimitRepository.existsByUserId(userId)) {
                log.info("User {} already has account limits assigned. Skipping.", userId);
                return;
            }
            
            // All new users are EMAIL_VERIFIED by default (as they verify email during registration)
            VerificationLevel level = VerificationLevel.EMAIL_VERIFIED;
            
            // Create and save the user's account limit record
            UserAccountLimit userLimit = new UserAccountLimit();
            userLimit.setUserId(userId);
            userLimit.setVerificationLevel(level);
            userAccountLimitRepository.save(userLimit);
            
            log.info("Default account limits (EMAIL_VERIFIED) assigned to user {}", userId);
        } catch (Exception e) {
            log.error("Error assigning limits to user {}: {}", userId, e.getMessage(), e);
            throw e; // Rethrow to be caught by the outer try-catch
        }
    }
    
    /**
     * Handle a Keycloak event JSON string
     * 
     * @param eventJson The JSON event string from Keycloak
     * @throws IOException If there's an error parsing the JSON
     */
    private void handleJsonEvent(String eventJson) throws IOException {
        JsonNode eventNode = objectMapper.readTree(eventJson);
        
        // Check if this is a REGISTER event
        String eventType = eventNode.path("type").asText();
        if (!"REGISTER".equals(eventType)) {
            log.info("Not a registration event. Skipping account limit assignment.");
            return;
        }
        
        // Extract userId from the event
        String userId = eventNode.path("userId").asText();
        
        // Assign limits to the extracted userId
        assignLimitsToUser(userId);
    }
}