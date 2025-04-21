package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;

import com.shizzy.moneytransfer.exception.InvalidFileFormatException;
import com.shizzy.moneytransfer.exception.InvalidRequestException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.KycVerification;
import com.shizzy.moneytransfer.repository.KycVerificationRepository;
import com.shizzy.moneytransfer.s3.S3Service;
import com.shizzy.moneytransfer.service.KycVerificationService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import static com.shizzy.moneytransfer.enums.VerificationStatus.*;

@Service
@RequiredArgsConstructor
public class KycVerificationServiceImpl implements KycVerificationService {

    private final KycVerificationRepository kycVerificationRepository;
    private final S3Service s3Service;
    private final DocumentUploadService documentUploadService;
    private final VerificationStatusService verificationStatusService;


    @Override
    public ApiResponse<String> uploadIdDocument(Authentication connectedUser,
                                                @NotNull(value = "Please provide a valid file") MultipartFile idDocument) throws InvalidFileFormatException {
        String userId = connectedUser.getName();
        String documentUrl = documentUploadService.uploadDocument(idDocument, userId, "id");

        KycVerification kyc = kycVerificationRepository.findByUserId(userId)
                .orElse(new KycVerification());
        kyc.setUserId(userId);
        kyc.setIdDocumentUrl(documentUrl);
        kyc.setIdVerificationStatus(PENDING);
        kycVerificationRepository.save(kyc);

        return createSuccessResponse("ID document uploaded successfully");
    }

    @Override
    public ApiResponse<String> uploadAddressDocument(Authentication connectedUser,
                                                     @NotNull(value = "Please provide a valid file") MultipartFile addressDocument) {
        String userId = connectedUser.getName();

        // Check if ID verification is completed
        KycVerification verification = kycVerificationRepository.findByUserId(userId)
                .orElse(new KycVerification());

        if (verification.getId() == null ) {
            throw new InvalidRequestException("Please complete ID verification first");
        }

        String documentUrl = documentUploadService.uploadDocument(addressDocument, userId, "address");

        verification.setUserId(userId);
        verification.setAddressDocumentUrl(documentUrl);
        verification.setAddressVerificationStatus(PENDING);
        kycVerificationRepository.save(verification);

        return createSuccessResponse("Address document uploaded successfully");
    }

    @Override
    public ApiResponse<String> approveAddressVerification(String userId) {
        verificationStatusService.updateVerificationStatus(userId, "id", APPROVED, null);

        return createSuccessResponse("Address verification approved successfully");
    }

    @Override
    public ApiResponse<String> rejectAddressVerification(@NotNull("user id is required") String userId,
                                                         @NotNull("please provide reason for rejection") String rejectionReason) {
        verificationStatusService.updateVerificationStatus(userId, "id", REJECTED, rejectionReason);

        return createSuccessResponse("Address verification rejected successfully");

    }

    @Override
    public ApiResponse<String> approveIdVerification(@NotNull("user id is required") String userId) {
        verificationStatusService.updateVerificationStatus(userId, "address", APPROVED, null);

        return createSuccessResponse("ID verification approved successfully");
    }

    @Override
    public ApiResponse<String> rejectIdVerification(@NotNull("user id is required") String userId,  @NotNull("please provide reason for rejection") String rejectionReason) {
        verificationStatusService.updateVerificationStatus(userId, "address", REJECTED, rejectionReason);

        return createSuccessResponse("ID verification rejected successfully");
    }

    @Override
    public ApiResponse<String> updateKyc(Authentication connectedUser, String addressDocumentUrl, String idDocumentUrl) {
        String userId = connectedUser.getName();
        KycVerification kyc = kycVerificationRepository.findByUserId(userId)
                .orElse(new KycVerification());

        if (idDocumentUrl != null) {
            kyc.setIdDocumentUrl(idDocumentUrl);
            kyc.setIdVerificationStatus(PENDING);
        }

        if (addressDocumentUrl != null) {
            kyc.setAddressDocumentUrl(addressDocumentUrl);
            kyc.setAddressVerificationStatus(PENDING);
        }

        kycVerificationRepository.save(kyc);

        return createSuccessResponse("KYC documents updated successfully");

    }

    @Override
    public ApiResponse<KycVerification> getKycStatus(Authentication connectedUser) {
        KycVerification kycVerification = kycVerificationRepository.findByUserId(connectedUser.getName())
                .orElseThrow(()-> new ResourceNotFoundException("KYC verification data not found"));

        return ApiResponse.<KycVerification>builder()
                .success(true)
                .data(kycVerification)
                .message("KYC verification data found")
                .build();
    }

    private ApiResponse<String> createSuccessResponse(String message) {
        return ApiResponse.<String>builder()
                .success(true)
                .message(message)
                .data(message)
                .build();
    }
}
