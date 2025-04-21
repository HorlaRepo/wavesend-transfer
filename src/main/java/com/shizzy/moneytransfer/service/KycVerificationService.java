package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.InvalidFileFormatException;
import com.shizzy.moneytransfer.model.KycVerification;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface KycVerificationService {
    ApiResponse<String> uploadIdDocument(Authentication connectedUser, MultipartFile idDocument) throws InvalidFileFormatException;
    ApiResponse<String> uploadAddressDocument(Authentication connectedUser, MultipartFile addressDocument);
    ApiResponse<String> approveAddressVerification(String userId);
    ApiResponse<String> rejectAddressVerification(String userId, String rejectionReason);
    ApiResponse<String> approveIdVerification(String userId);
    ApiResponse<String> rejectIdVerification(String userId, String rejectionReason);
    ApiResponse<String> updateKyc(Authentication connectedUser, String addressDocumentUrl, String idDocumentUrl);
    ApiResponse<KycVerification> getKycStatus(Authentication connectedUser);
}
