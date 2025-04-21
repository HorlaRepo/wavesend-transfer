package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.InvalidFileFormatException;
import com.shizzy.moneytransfer.model.KycVerification;
import com.shizzy.moneytransfer.service.KycVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("kyc-verification")
public class KycVerificationController {

        private final KycVerificationService kycVerificationService;

        @GetMapping(value = "/status")
        public ApiResponse<KycVerification> getKycStatus(Authentication authentication) {
            return kycVerificationService.getKycStatus(authentication);
        }

        @PostMapping(value = "/id-document", consumes = "multipart/form-data")
        public ApiResponse<String> uploadIdDocument(Authentication authentication, @RequestParam ("file") MultipartFile idDocument) throws InvalidFileFormatException {
            return kycVerificationService.uploadIdDocument(authentication, idDocument);
        }

        @PostMapping(value = "/address-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ApiResponse<String> uploadAddressDocument(Authentication authentication, @RequestParam ("file") @Valid MultipartFile addressDocument) {
            return kycVerificationService.uploadAddressDocument(authentication, addressDocument);
        }

        @PreAuthorize("hasRole('ROLE_ADMIN')")
        @PutMapping(value = "/approve-id-verification")
        public ApiResponse<String> approveIdVerification(@RequestParam("userId") String userId) {
            return kycVerificationService.approveIdVerification(userId);
        }

        @PreAuthorize("hasRole('ROLE_ADMIN')")
        @PutMapping(value = "/reject-id-verification")
        public ApiResponse<String> rejectIdVerification(@RequestParam("userId") @Valid String userId, @RequestParam("rejectionReason") @Valid String rejectionReason) {
            return kycVerificationService.rejectIdVerification(userId, rejectionReason);
        }

        @PreAuthorize("hasRole('ROLE_ADMIN')")
        @PutMapping(value = "/approve-address-verification")
        public ApiResponse<String> approveAddressVerification(@RequestParam("userId") @Valid String userId) {
            return kycVerificationService.approveAddressVerification(userId);
        }

        @PreAuthorize("hasRole('ROLE_ADMIN')")
        @PutMapping(value = "/reject-address-verification")
        public ApiResponse<String> rejectAddressVerification(@RequestParam("userId") @Valid String userId, @RequestParam("rejectionReason") @Valid String rejectionReason) {
            return kycVerificationService.rejectAddressVerification(userId, rejectionReason);
        }

}
