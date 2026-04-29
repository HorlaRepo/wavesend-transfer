package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.enums.EmailTemplateName;
import com.shizzy.moneytransfer.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test-email")
@RequiredArgsConstructor
@Slf4j
public class EmailTestController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendTestEmail(@RequestParam String to) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("company_logo", "https://wavesend.cc/logo.png");
            data.put("recipient_name", "Test User");
            data.put("currency_symbol", "$");
            data.put("amount", "100.00");
            data.put("reference_number", "TEST-" + System.currentTimeMillis());
            data.put("transaction_date", LocalDateTime.now().toString());
            data.put("description", "Test transaction from Mailtrap");
            data.put("status", "SUCCESS");
            data.put("new_balance", "1500.00");
            data.put("dashboard_url", "https://app.wavesend.cc/account/transactions");
            data.put("facebook_url", "https://facebook.com/wavesend");
            data.put("twitter_url", "https://twitter.com/wavesend");
            data.put("instagram_url", "https://instagram.com/wavesend");
            data.put("current_year", String.valueOf(LocalDateTime.now().getYear()));
            data.put("source", "Wallet");

            emailService.sendEmail(to, data, EmailTemplateName.DEPOSIT, "Test Email - WaveSend via Mailtrap");

            log.info("Test email sent successfully to: {}", to);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Test email sent to " + to);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (MessagingException e) {
            log.error("Failed to send test email: {}", e.getMessage(), e);

            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to send email: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}
