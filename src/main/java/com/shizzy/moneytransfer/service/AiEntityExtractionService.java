package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.dto.ScheduledTransferDetails;
import com.shizzy.moneytransfer.dto.TransferDetails;
import com.shizzy.moneytransfer.dto.WithdrawalDetails;
import reactor.core.publisher.Mono;

/**
 * Service to extract structured financial transaction details from natural language
 */
public interface AiEntityExtractionService {
    
    /**
     * Extract transfer details from a user message
     * 
     * @param userId The ID of the user
     * @param message The message from the user
     * @return Extracted transfer details
     */
    Mono<TransferDetails> extractTransferDetails(String userId, String message);
    
    /**
     * Extract scheduled transfer details from a user message
     * 
     * @param userId The ID of the user
     * @param message The message from the user
     * @return Extracted scheduled transfer details
     */
    Mono<ScheduledTransferDetails> extractScheduledTransferDetails(String userId, String message);
    
    /**
     * Extract withdrawal details from a user message
     * 
     * @param userId The ID of the user
     * @param message The message from the user
     * @return Extracted withdrawal details
     */
    Mono<WithdrawalDetails> extractWithdrawalDetails(String userId, String message);
}