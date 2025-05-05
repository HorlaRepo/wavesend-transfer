package com.shizzy.moneytransfer.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shizzy.moneytransfer.api.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Filter that validates incoming requests to detect and prevent common web attacks
 * such as SQL injection and XSS attacks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RequestValidationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    
    // Pattern to detect potential SQL injection attempts
    private static final Pattern SQL_INJECTION_PATTERN = 
            Pattern.compile("['\"\\\\;]|(--)|(/\\*)|(%27)|(\\)\\s+or)", Pattern.CASE_INSENSITIVE);
    
    // Pattern to detect potential XSS attempts
    private static final Pattern XSS_PATTERN = 
            Pattern.compile("<script|javascript:|on\\w+=", Pattern.CASE_INSENSITIVE);
    
    // Paths that need protection - focus on financial transaction endpoints
    private static final List<String> PROTECTED_PATHS = List.of(
            "/transfer", "/withdraw", "/deposit", "/account", 
            "/beneficiaries", "/scheduled-transfers", "/payment"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Only validate requests to protected paths
        if (isProtectedPath(path)) {
            // Check for potential SQL Injection or XSS attacks in parameters
            if (containsMaliciousContent(request)) {
                log.warn("Potential attack detected from IP: {}, Path: {}", 
                        request.getRemoteAddr(), path);
                
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                
                ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                        .success(false)
                        .message("Invalid request parameters")
                        .build();
                
                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Determines if the given path should be protected by this filter
     * 
     * @param path the request URI path
     * @return true if this is a protected financial endpoint
     */
    private boolean isProtectedPath(String path) {
        return PROTECTED_PATHS.stream().anyMatch(path::contains);
    }
    
    /**
     * Checks request parameters for potential malicious content
     * 
     * @param request the HTTP request
     * @return true if potentially malicious content is detected
     */
    private boolean containsMaliciousContent(HttpServletRequest request) {
        Enumeration<String> paramNames = request.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] values = request.getParameterValues(paramName);
            
            for (String value : values) {
                if (value != null && (SQL_INJECTION_PATTERN.matcher(value).find() || 
                        XSS_PATTERN.matcher(value).find())) {
                    log.warn("Potentially malicious content detected in parameter '{}': {}", 
                            paramName, value);
                    return true;
                }
            }
        }
        
        // Also check request headers for common attack vectors
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            
            // Skip standard headers that might contain false positives
            if (headerName.equalsIgnoreCase("cookie") || 
                headerName.equalsIgnoreCase("authorization") ||
                headerName.equalsIgnoreCase("user-agent")) {
                continue;
            }
            
            if (headerValue != null && (SQL_INJECTION_PATTERN.matcher(headerValue).find() || 
                    XSS_PATTERN.matcher(headerValue).find())) {
                log.warn("Potentially malicious content detected in header '{}': {}", 
                        headerName, headerValue);
                return true;
            }
        }
        
        return false;
    }
}