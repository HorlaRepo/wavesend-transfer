package com.shizzy.moneytransfer.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
@Slf4j
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final Environment environment;
    
    @Value("${app.frontend.url:https://app.wavesend.cc}")
    private String frontendUrl;
    
    public SecurityHeadersFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Basic security headers that rarely cause issues
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Cache control (fine to keep)
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        
        // Allow frames from same origin, which helps with frontend components that use iframes internally
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        
        // Only apply HSTS in production environment
        boolean isProduction = !Arrays.asList(environment.getActiveProfiles()).contains("dev") && 
                               !Arrays.asList(environment.getActiveProfiles()).contains("local");
        
        if (isProduction) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        
        // Build appropriate CSP for the environment
        String csp = buildContentSecurityPolicy();
        response.setHeader("Content-Security-Policy", csp);
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Build Content Security Policy appropriate for the current environment
     */
    private String buildContentSecurityPolicy() {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev") || 
                        Arrays.asList(environment.getActiveProfiles()).contains("local");
        
        // Base domains needed
        String frontendDomain = frontendUrl.replaceAll("https?://", "");
        
        // Development vs Production CSP
        if (isDev) {
            // More permissive CSP for development
            return "default-src 'self'; " +
                   "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://localhost:4200; " +
                   "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                   "img-src 'self' data: https:; " +
                   "font-src 'self' https://fonts.gstatic.com; " +
                   "connect-src 'self' http://localhost:4200 ws://localhost:4200 wss://localhost:4200; " +
                   "frame-src 'self' https://www.google.com; " +  // For reCAPTCHA
                   "object-src 'none'; " +
                   "base-uri 'self';";
        } else {
            // Production CSP - more restrictive but allows necessary resources
            return "default-src 'self'; " +
                   "script-src 'self' https://" + frontendDomain + " https://www.google.com https://www.gstatic.com; " +
                   "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +  // Unsafe-inline for styles
                   "img-src 'self' data: https:; " +
                   "font-src 'self' https://fonts.gstatic.com; " +
                   "connect-src 'self' https://" + frontendDomain + "; " +
                   "frame-src 'self' https://www.google.com; " +  // For reCAPTCHA
                   "object-src 'none'; " +
                   "frame-ancestors 'self'; " +
                   "base-uri 'self';";
        }
    }
}