package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.enums.VerificationStatus;
import com.shizzy.moneytransfer.model.KycVerification;
import com.shizzy.moneytransfer.repository.KycVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.shizzy.moneytransfer.enums.VerificationStatus.REJECTED;

@Service
@RequiredArgsConstructor
public class VerificationStatusService {
    private final KycVerificationRepository kycVerificationRepository;

    public void updateVerificationStatus(String userId, String documentType,
                                         VerificationStatus status,
                                         String rejectionReason) {
        KycVerification kyc = kycVerificationRepository.findByUserId(userId)
                .orElse(new KycVerification());
        kyc.setUserId(userId);

        if ("id".equals(documentType)) {
            kyc.setIdVerificationStatus(status);
            if (status == REJECTED) {
                kyc.setIdRejectionReason(rejectionReason);
            }
        } else if ("address".equals(documentType)) {
            kyc.setAddressVerificationStatus(status);
            if (status == REJECTED) {
                kyc.setAddressRejectionReason(rejectionReason);
            }
        }

        kycVerificationRepository.save(kyc);
    }
}
