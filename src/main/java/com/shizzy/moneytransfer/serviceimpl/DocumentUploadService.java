package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentUploadService {
    private final S3Service s3Service;

    public String uploadDocument(MultipartFile document, String userId, String documentType) {
        s3Service.validateKycDocument(document);
        String key = String.format("kyc-documents/%s/%s-docs/%s",
                userId, documentType, document.getOriginalFilename());
        return s3Service.uploadFileToS3(document, key);
    }
}
