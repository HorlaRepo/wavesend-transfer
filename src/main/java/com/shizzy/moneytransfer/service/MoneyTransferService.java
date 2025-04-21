package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.CreateTransactionRequestBody;
import com.shizzy.moneytransfer.dto.TransactionResponseDTO;
import com.shizzy.moneytransfer.dto.TransferInitiationResponse;
import com.shizzy.moneytransfer.dto.TransferVerificationRequest;

public interface MoneyTransferService {
    ApiResponse<TransactionResponseDTO> transfer(CreateTransactionRequestBody request);

    /**
     * Initiate a transfer - triggers OTP
     */
    ApiResponse<TransferInitiationResponse> initiateTransfer(CreateTransactionRequestBody requestBody, String userId);
    
    /**
     * Complete a transfer with OTP verification
     */
    ApiResponse<TransactionResponseDTO> verifyAndTransfer(TransferVerificationRequest request, String userId);
}
