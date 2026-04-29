package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.AiMessageRequest;
import com.shizzy.moneytransfer.model.User;
import com.shizzy.moneytransfer.service.AiFinancialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("ai-assistant")
@RequiredArgsConstructor
public class AiFinancialAssistantController {

    private final AiFinancialService aiFinancialService;

    @PostMapping(value = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> processMessage(
            @AuthenticationPrincipal User principal,
            @RequestBody AiMessageRequest request) {

        if (principal == null) {
            log.error("Unauthenticated request to AI assistant");
            return ResponseEntity.status(401).body(
                ApiResponse.<String>builder()
                    .success(false)
                    .message("Authentication required")
                    .build()
            );
        }

        String userId = principal.getUserId().toString();
        log.info("Received message from user {}", userId);

        ApiResponse<String> response = aiFinancialService.processUserMessage(userId, request.getMessage())
                .doOnError(error -> log.error("Error processing AI message: {}", error.getMessage(), error))
                .block();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/conversation")
    public ResponseEntity<ApiResponse<Void>> clearConversation(@AuthenticationPrincipal User principal) {
        String userId = principal.getUserId().toString();
        aiFinancialService.clearConversation(userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Conversation cleared")
                .build());
    }
}