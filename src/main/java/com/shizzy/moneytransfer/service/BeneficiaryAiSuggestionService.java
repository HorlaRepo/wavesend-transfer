package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.BeneficiaryAiSuggestion;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BeneficiaryAiSuggestionService {
    CompletableFuture<Void> generateSuggestionsForUser(String userId);
    
    ApiResponse<List<BeneficiaryAiSuggestion>> getUserSuggestions(String userId);
    
    ApiResponse<String> dismissSuggestion(String userId, Long suggestionId);
    
    void deleteExpiredSuggestions();
}