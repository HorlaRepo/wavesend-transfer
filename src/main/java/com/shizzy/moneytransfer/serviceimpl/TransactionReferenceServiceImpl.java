package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.TransactionReference;
import com.shizzy.moneytransfer.repository.TransactionReferenceRepository;
import com.shizzy.moneytransfer.service.TransactionReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class TransactionReferenceServiceImpl implements TransactionReferenceService {

    private final TransactionReferenceRepository transactionReferenceRepository;

    @Override
    public void saveTransactionReference(TransactionReference transactionReference, String suffix) {
        if (suffix == null) suffix = "";
        transactionReference.setReferenceNumber(generateUniqueReferenceNumber() + suffix);
        transactionReferenceRepository.save(transactionReference);
    }

    @Override
    public void saveTransactionReference(String referenceNumber) {
        TransactionReference transactionReference = TransactionReference.builder()
                .referenceNumber(referenceNumber)
                .build();
        transactionReferenceRepository.save(transactionReference);
    }

    @Override
    public ApiResponse<TransactionReference> getTransactionReferenceByReferenceNumber(String referenceNumber) {
        TransactionReference transactionReference = transactionReferenceRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new RuntimeException("Transaction reference not found"));

        return ApiResponse.<TransactionReference>builder()
                .success(true)
                .data(transactionReference)
                .build();
    }

    @Override
    public String generateUniqueReferenceNumber() {
        String referenceNumber = generateReferenceNumber();
        while (transactionReferenceRepository.existsByReferenceNumber(referenceNumber)) {
            referenceNumber = generateReferenceNumber();
        }
        return referenceNumber;
    }


    private String generateReferenceNumber() {
        SecureRandom rand = new SecureRandom();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder referenceNumber = new StringBuilder();

        for (int i = 0; i < 16; i++) {
            int index = rand.nextInt(characters.length());
            referenceNumber.append(characters.charAt(index));
        }

        return referenceNumber.toString();
    }


}
