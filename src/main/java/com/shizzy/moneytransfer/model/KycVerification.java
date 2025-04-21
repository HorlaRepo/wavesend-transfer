package com.shizzy.moneytransfer.model;

import com.shizzy.moneytransfer.enums.VerificationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class KycVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String idCountry;
    private String idType;
    private String idNumber;
    private LocalDate expiryDate;
    private String addressDocumentUrl;
    private String idDocumentUrl;
    private String idRejectionReason;
    private String addressRejectionReason;
    private VerificationStatus addressVerificationStatus;
    private VerificationStatus idVerificationStatus;

}
