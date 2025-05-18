package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.enums.VerificationLevel;
import com.shizzy.moneytransfer.exception.InvalidFileFormatException;
import com.shizzy.moneytransfer.exception.InvalidRequestException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.KycVerification;
import com.shizzy.moneytransfer.model.UserAccountLimit;
import com.shizzy.moneytransfer.repository.KycVerificationRepository;
import com.shizzy.moneytransfer.repository.UserAccountLimitRepository;
import com.shizzy.moneytransfer.s3.S3Service;
import com.shizzy.moneytransfer.service.KycVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static com.shizzy.moneytransfer.enums.VerificationStatus.*;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycVerificationServiceImpl implements KycVerificationService {

    private final KycVerificationRepository kycVerificationRepository;
    private final S3Service s3Service;
    private final DocumentUploadService documentUploadService;
    private final VerificationStatusService verificationStatusService;
    private final UserAccountLimitRepository userAccountLimitRepository;

    @Override
    public ApiResponse<String> uploadIdDocument(Authentication connectedUser,
            @NotNull(value = "Please provide a valid file") MultipartFile idDocument)
            throws InvalidFileFormatException {
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

        if (verification.getId() == null) {
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
        verificationStatusService.updateVerificationStatus(userId, "address", APPROVED, null);

        // Update user's verification level to FULLY_VERIFIED if ID is also verified
        KycVerification kyc = kycVerificationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC verification not found for user: " + userId));

        if (kyc.getIdVerificationStatus() == APPROVED) {
            updateUserVerificationLevel(userId, VerificationLevel.FULLY_VERIFIED);
        }

        return createSuccessResponse("Address verification approved successfully");
    }

    @Override
    public ApiResponse<String> rejectAddressVerification(@NotNull("user id is required") String userId,
            @NotNull("please provide reason for rejection") String rejectionReason) {
        verificationStatusService.updateVerificationStatus(userId, "id", REJECTED, rejectionReason);

        return createSuccessResponse("Address verification rejected successfully");

    }

    @Override
    public ApiResponse<String> approveIdVerification(String userId) {
        verificationStatusService.updateVerificationStatus(userId, "id", APPROVED, null);

        // Update user's verification level to ID_VERIFIED
        updateUserVerificationLevel(userId, VerificationLevel.ID_VERIFIED);

        return createSuccessResponse("ID verification approved successfully");
    }

    @Override
    public ApiResponse<String> rejectIdVerification(@NotNull("user id is required") String userId,
            @NotNull("please provide reason for rejection") String rejectionReason) {
        verificationStatusService.updateVerificationStatus(userId, "address", REJECTED, rejectionReason);

        return createSuccessResponse("ID verification rejected successfully");
    }

    @Override
    public ApiResponse<String> updateKyc(Authentication connectedUser, String addressDocumentUrl,
            String idDocumentUrl) {
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
                .orElseThrow(() -> new ResourceNotFoundException("KYC verification data not found"));

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

    private void updateUserVerificationLevel(String userId, VerificationLevel level) {
        UserAccountLimit userLimit = userAccountLimitRepository.findByUserId(userId)
                .orElse(new UserAccountLimit());

        userLimit.setUserId(userId);
        userLimit.setVerificationLevel(level);
        userLimit.setCreatedAt(LocalDateTime.now());
        userAccountLimitRepository.save(userLimit);

        log.info("Updated user {} verification level to {}", userId, level);
    }
}
