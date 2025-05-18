package com.shizzy.moneytransfer.config;

import com.shizzy.moneytransfer.api.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

    // Cache of rate limit buckets per user
    @Bean
    public Map<String, Bucket> userBuckets() {
        return new ConcurrentHashMap<>();
    }

    // Cache of buckets for anonymous requests (IP-based)
    @Bean
    public Map<String, Bucket> ipBuckets() {
        return new ConcurrentHashMap<>();
    }

    // Rate limit for authenticated API requests (more generous)
    @Bean
    public Bucket createAuthenticatedBucket() {
        // Create bandwidth limits
        Bandwidth limit1 = Bandwidth.classic(80, Refill.intervally(60, Duration.ofMinutes(1)));
        Bandwidth limit2 = Bandwidth.classic(800, Refill.intervally(800, Duration.ofHours(1)));

        // Create and return bucket with limits
        return Bucket.builder()
                .addLimit(limit1)
                .addLimit(limit2)
                .build();
    }

    // Rate limit for API requests involving money transfers
    @Bean
    public Bucket createFinancialTransactionBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    // More restrictive rate limit for unauthenticated API requests
    @Bean
    public Bucket createAnonymousBucket() {
        Bandwidth limit = Bandwidth.classic(30, Refill.intervally(20, Duration.ofMinutes(1)));

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    // Very restrictive rate limit for sensitive operations
    @Bean
    public Bucket createSensitiveOperationBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(1)));

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    // Rate limit for failed login attempts (to prevent brute force)
    @Bean
    public Bucket createLoginAttemptBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(10)));

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    // Standard response for rate limit exceeded

    public static ResponseEntity<ApiResponse<String>> createRateLimitExceededResponse() {
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message("Rate limit exceeded. Please try again later.")
                .build();

        return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    }
}