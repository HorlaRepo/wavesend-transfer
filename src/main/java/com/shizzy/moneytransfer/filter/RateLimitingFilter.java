package com.shizzy.moneytransfer.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.config.RateLimitingConfig;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> userBuckets;
    private final Map<String, Bucket> ipBuckets;
    private final Bucket authenticatedBucket;
    private final Bucket anonymousBucket;
    private final Bucket financialTransactionBucket;
    private final Bucket sensitiveOperationBucket;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(
            Map<String, Bucket> userBuckets,
            Map<String, Bucket> ipBuckets,
            @Qualifier("createAuthenticatedBucket") Bucket authenticatedBucket,
            @Qualifier("createAnonymousBucket") Bucket anonymousBucket,
            @Qualifier("createFinancialTransactionBucket") Bucket financialTransactionBucket,
            @Qualifier("createSensitiveOperationBucket") Bucket sensitiveOperationBucket,
            ObjectMapper objectMapper) {
        this.userBuckets = userBuckets;
        this.ipBuckets = ipBuckets;
        this.authenticatedBucket = authenticatedBucket;
        this.anonymousBucket = anonymousBucket;
        this.financialTransactionBucket = financialTransactionBucket;
        this.sensitiveOperationBucket = sensitiveOperationBucket;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Skip rate limiting for specific paths (like webhooks and health checks)
        if (shouldSkipRateLimiting(path)) {
            log.debug("Skipping rate limiting for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // Get user ID if authenticated, otherwise use IP address
        String identifier = getIdentifier(request);
        String clientIp = extractClientIp(request);

        // Log request details
        log.info("Request: {} {} | From: {} | Identifier: {}",
                method, path, clientIp, identifier);

        // Select the appropriate bucket based on the request type
        Bucket targetBucket = selectBucket(request, identifier);
        String bucketType = getBucketTypeName(request, identifier);

        // Try to consume a token from the bucket
        ConsumptionProbe probe = targetBucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Set rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.addHeader("X-Rate-Limit-Reset",
                    String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));

            // Log the successful request with rate limit info
            log.info("Rate limit OK for {} | Bucket: {} | Remaining: {}",
                    identifier, bucketType, probe.getRemainingTokens());

            // Allow the request to proceed
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit EXCEEDED for {} | Bucket: {} | Reset in: {} seconds",
                    identifier, bucketType, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));

            // Set rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", "0");
            response.addHeader("X-Rate-Limit-Reset",
                    String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));
            response.addHeader("Retry-After",
                    String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));

            // Return 429 Too Many Requests
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ResponseEntity<ApiResponse<String>> apiResponse = RateLimitingConfig.createRateLimitExceededResponse();
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        }

        // Log total request processing time
        long duration = System.currentTimeMillis() - startTime;
        log.debug("Request processed in {} ms: {} {} | From: {}",
                duration, method, path, clientIp);
    }

    private boolean shouldSkipRateLimiting(String path) {
        return path.startsWith("/health") ||
                path.contains("/webhook") ||
                path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api/v1/transactions") || // Transaction history
                path.startsWith("/api/v1/user/profile") || // Profile info
                path.contains("/public") || // Public endpoints
                path.equals("/api/v1/payment/deposit"); // Deposit page loads
    }

    private String getIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getPrincipal().toString().equals("anonymousUser")) {
            return "user:" + authentication.getName();
        } else {
            // For unauthenticated requests, use client's IP address
            String ipAddress = extractClientIp(request);
            return "ip:" + ipAddress;
        }
    }

    /**
     * Extracts the client IP address considering various headers and proxies
     */
    private String extractClientIp(HttpServletRequest request) {
        String ipAddress = null;

        // Check for X-Forwarded-For header first (standard proxy header)
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2...)
            // The first one is the original client
            String[] ips = forwardedFor.split(",");
            ipAddress = ips[0].trim();

            log.debug("IP from X-Forwarded-For: {}", ipAddress);
            return ipAddress;
        }

        // Check other common headers
        ipAddress = request.getHeader("CF-Connecting-IP"); // Cloudflare
        if (ipAddress != null && !ipAddress.isEmpty()) {
            log.debug("IP from CF-Connecting-IP: {}", ipAddress);
            return ipAddress;
        }

        ipAddress = request.getHeader("True-Client-IP");
        if (ipAddress != null && !ipAddress.isEmpty()) {
            log.debug("IP from True-Client-IP: {}", ipAddress);
            return ipAddress;
        }

        ipAddress = request.getHeader("X-Real-IP"); // Used by nginx
        if (ipAddress != null && !ipAddress.isEmpty()) {
            log.debug("IP from X-Real-IP: {}", ipAddress);
            return ipAddress;
        }

        // Fall back to the remote address from the request
        ipAddress = request.getRemoteAddr();
        log.debug("IP from RemoteAddr: {}", ipAddress);
        return ipAddress;
    }

    private Bucket selectBucket(HttpServletRequest request, String identifier) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Check if authenticated
        boolean isAuthenticated = identifier.startsWith("user:");

        // ONLY apply strict rate limiting for specific financial operations
        if ("POST".equals(method) && (path.contains("/scheduled-transfers/initiate") ||
                path.contains("/scheduled-transfers/verify") ||
                path.contains("/transfers/initiate") ||
                path.contains("/transfers/verify") ||
                path.contains("/withdrawals/initiate") ||
                path.contains("/withdrawals/verify"))) {

            log.info("Applying strict rate limit for critical financial operation: {}", path);
            return financialTransactionBucket;
        }

        // For authenticated users, use a more permissive bucket
        if (isAuthenticated) {
            return userBuckets.computeIfAbsent(identifier, k -> authenticatedBucket);
        }

        // For anonymous users, still use IP-based limiting but with anonymous bucket
        return ipBuckets.computeIfAbsent(identifier, k -> anonymousBucket);
    }

    /**
     * Get a human-readable name of the bucket type for logging purposes
     */
    private String getBucketTypeName(HttpServletRequest request, String identifier) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Match the same pattern as in selectBucket
        if ("POST".equals(method) && (path.contains("/scheduled-transfers/initiate") ||
                path.contains("/scheduled-transfers/verify") ||
                path.contains("/transfers/initiate") ||
                path.contains("/transfers/verify") ||
                path.contains("/withdrawals/initiate") ||
                path.contains("/withdrawals/verify"))) {
            return "CRITICAL_FINANCIAL_TRANSACTION";
        }

        return identifier.startsWith("user:") ? "AUTHENTICATED" : "ANONYMOUS";
    }
}