package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.BeneficiaryAiSuggestion;
import com.shizzy.moneytransfer.model.User;
import com.shizzy.moneytransfer.service.BeneficiaryAiSuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/beneficiary/suggestions")
@RequiredArgsConstructor
public class BeneficiaryAiSuggestionController {

    private final BeneficiaryAiSuggestionService suggestionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BeneficiaryAiSuggestion>>> getUserSuggestions(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(suggestionService.getUserSuggestions(user.getUserId().toString()));
    }

    @PostMapping("/{suggestionId}/dismiss")
    public ResponseEntity<ApiResponse<String>> dismissSuggestion(
            @PathVariable Long suggestionId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(suggestionService.dismissSuggestion(
                user.getUserId().toString(), suggestionId));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<String>> generateSuggestions(@AuthenticationPrincipal User user) {
        suggestionService.generateSuggestionsForUser(user.getUserId().toString());
        
        return ResponseEntity.accepted().body(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Suggestion generation started")
                        .data("Check back shortly for new suggestions")
                        .build());
    }
}