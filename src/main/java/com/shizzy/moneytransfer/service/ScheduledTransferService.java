package com.shizzy.moneytransfer.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.PageResponse;
import com.shizzy.moneytransfer.dto.ScheduledTransferInitiationResponse;
import com.shizzy.moneytransfer.dto.ScheduledTransferRequestDTO;
import com.shizzy.moneytransfer.dto.ScheduledTransferResponseDTO;
import com.shizzy.moneytransfer.dto.ScheduledTransferVerificationRequest;

public interface ScheduledTransferService {
    ApiResponse<ScheduledTransferResponseDTO> scheduleTransfer(ScheduledTransferRequestDTO request, String uderId);

    // New OTP methods
    ApiResponse<ScheduledTransferInitiationResponse> initiateScheduledTransfer(
            ScheduledTransferRequestDTO request, String userId);
            
    ApiResponse<ScheduledTransferResponseDTO> verifyAndScheduleTransfer(
            ScheduledTransferVerificationRequest request, String userId);
    
    // Updated method to support pagination
    ApiResponse<PageResponse<ScheduledTransferResponseDTO>> getUserScheduledTransfers(String userEmail, Pageable pageable);
    
    // Legacy method for backward compatibility
    ApiResponse<List<ScheduledTransferResponseDTO>> getUserScheduledTransfers(String userEmail);
    
    ApiResponse<ScheduledTransferResponseDTO> cancelScheduledTransfer(Long transferId, String userEmail);
    
    void processScheduledTransfers();
    
    ApiResponse<List<ScheduledTransferResponseDTO>> getRecurringTransferSeries(Long parentId, String userEmail);
    
    ApiResponse<ScheduledTransferResponseDTO> cancelRecurringSeries(Long parentId, String userEmail);
    
    ApiResponse<ScheduledTransferResponseDTO> updateRecurringTransfer(Long transferId, ScheduledTransferRequestDTO request, String userEmail);
}