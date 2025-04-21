package com.shizzy.moneytransfer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.shizzy.moneytransfer.service.OtpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulingConfig {

    private final OtpService otpService;
    
    // Run every 10 minutes to clean expired entries
    @Scheduled(fixedRate = 600000)
    public void cleanExpiredOtps() {
        log.debug("Running scheduled task to clean expired OTPs");
        otpService.clearExpiredOtps();
    }
}