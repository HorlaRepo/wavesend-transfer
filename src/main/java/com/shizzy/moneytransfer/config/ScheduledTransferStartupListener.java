package com.shizzy.moneytransfer.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.shizzy.moneytransfer.service.ScheduledTransferPublisherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTransferStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final ScheduledTransferPublisherService scheduledTransferPublisherService;
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Application started - checking for missed scheduled transfers...");
        scheduledTransferPublisherService.publishMissedTransfers();
    }
}