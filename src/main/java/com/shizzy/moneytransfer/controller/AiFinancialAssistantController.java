package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.AiMessageRequest;
import com.shizzy.moneytransfer.service.AiFinancialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("ai-assistant")
@RequiredArgsConstructor
public class AiFinancialAssistantController {

    private final AiFinancialService aiFinancialService;

    @PostMapping(value = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<ApiResponse<String>>> processMessage(
            @AuthenticationPrincipal Jwt principal,
            @RequestBody AiMessageRequest request) {

        String userId = principal.getSubject();
        log.info("Received message from user {}", userId);

        return aiFinancialService.processUserMessage(userId, request.getMessage())
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/conversation")
    public ResponseEntity<ApiResponse<Void>> clearConversation(@AuthenticationPrincipal Jwt principal) {
        String userId = principal.getSubject();
        aiFinancialService.clearConversation(userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Conversation cleared")
                .build());
    }
}