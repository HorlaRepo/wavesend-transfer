package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.UserBeneficiaryRequest;
import com.shizzy.moneytransfer.dto.UserBeneficiaryResponse;
import com.shizzy.moneytransfer.dto.UserBeneficiariesResponse;
import com.shizzy.moneytransfer.exception.DuplicateResourceException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.UserBeneficiary;
import com.shizzy.moneytransfer.model.UserBeneficiaries;
import com.shizzy.moneytransfer.repository.UserBeneficiariesRepository;
import com.shizzy.moneytransfer.service.UserBeneficiariesService;
import com.shizzy.moneytransfer.util.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBeneficiariesServiceImpl implements UserBeneficiariesService {

    private final UserBeneficiariesRepository beneficiariesRepository;

    @Override
    @Transactional
    @CacheEvict(value = {CacheNames.USER_BENEFICIARIES}, key = "#connectedUser.name")
    public ApiResponse<UserBeneficiaryResponse> addBeneficiary(Authentication connectedUser,
            UserBeneficiaryRequest beneficiaryRequest) {

        log.debug("Adding beneficiary for user: {}", connectedUser.getName());

        // Validate the beneficiary
        UserBeneficiary beneficiary = UserBeneficiary.builder()
                .name(beneficiaryRequest.getName())
                .email(beneficiaryRequest.getEmail())
                .build();

        // Get or create user beneficiaries
        UserBeneficiaries userBeneficiaries = getUserBeneficiaries(connectedUser.getName());

        // Check for duplicate
        if (isDuplicateBeneficiary(userBeneficiaries, beneficiary)) {
            throw new DuplicateResourceException("Beneficiary already exists");
        }

        // Add and save
        userBeneficiaries.getBeneficiaries().add(beneficiary);
        beneficiariesRepository.save(userBeneficiaries);

        UserBeneficiaryResponse response = mapToUserBeneficiaryResponse(beneficiary);

        return ApiResponse.<UserBeneficiaryResponse>builder()
                .success(true)
                .data(response)
                .message("Beneficiary added successfully")
                .build();

    }

    @Override
    @Transactional
    @CacheEvict(value = {CacheNames.USER_BENEFICIARIES, CacheNames.SINGLE_BENEFICIARY}, 
                key = "#connectedUser.name", allEntries = true)
    public ApiResponse<String> deleteBeneficiary(Authentication connectedUser, Long beneficiaryId) {
        log.debug("Deleting beneficiary {} for user: {}", beneficiaryId, connectedUser.getName());
        
        UserBeneficiaries userBeneficiaries = getUserBeneficiariesOrThrow(connectedUser.getName());

        // Find the beneficiary first
        UserBeneficiary beneficiaryToRemove = userBeneficiaries.getBeneficiaries().stream()
                .filter(b -> b.getId().equals(beneficiaryId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found"));

        // Remove the beneficiary from the list
        userBeneficiaries.getBeneficiaries().remove(beneficiaryToRemove);

        // Save the parent entity
        beneficiariesRepository.save(userBeneficiaries);

        return createSuccessResponse("Beneficiary deleted successfully");
    }


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.SINGLE_BENEFICIARY, 
              key = "#connectedUser.name + ':' + #beneficiaryId", 
              unless = "#result == null || #result.data == null")
    public ApiResponse<UserBeneficiaryResponse> getBeneficiary(Authentication connectedUser, Long beneficiaryId) {
        log.debug("Fetching beneficiary {} for user: {}", beneficiaryId, connectedUser.getName());
        
        UserBeneficiaries userBeneficiaries = getUserBeneficiariesOrThrow(connectedUser.getName());

        UserBeneficiary beneficiary = userBeneficiaries.getBeneficiaries().stream()
                .filter(b -> b.getId().equals(beneficiaryId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found"));

        UserBeneficiaryResponse response = mapToUserBeneficiaryResponse(beneficiary);

        return ApiResponse.<UserBeneficiaryResponse>builder()
                .success(true)
                .data(response)
                .message("Beneficiary retrieved successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.USER_BENEFICIARIES, 
              key = "#connectedUser.name", 
              unless = "#result == null || #result.data == null")
    public ApiResponse<UserBeneficiariesResponse> getBeneficiaries(Authentication connectedUser) {
        log.debug("Fetching all beneficiaries for user: {}", connectedUser.getName());
        
        List<UserBeneficiary> beneficiaries = getUserBeneficiaries(connectedUser.getName()).getBeneficiaries();
        
        List<UserBeneficiaryResponse> beneficiaryResponses = beneficiaries.stream()
                .map(this::mapToUserBeneficiaryResponse)
                .collect(Collectors.toList());

        UserBeneficiariesResponse response = UserBeneficiariesResponse.builder()
                .beneficiaries(beneficiaryResponses)
                .build();

        return ApiResponse.<UserBeneficiariesResponse>builder()
                .success(true)
                .data(response)
                .message("Beneficiaries retrieved successfully")
                .build();
    }

    private boolean isDuplicateBeneficiary(UserBeneficiaries userBeneficiaries, UserBeneficiary beneficiary) {
        return userBeneficiaries.getBeneficiaries().stream()
                .anyMatch(b -> b.getEmail().equals(beneficiary.getEmail()) &&
                        b.getName().equals(beneficiary.getName()));
    }

    private UserBeneficiaries getUserBeneficiariesOrThrow(String userId) {
        return beneficiariesRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No beneficiaries found"));
    }

    private UserBeneficiaries getUserBeneficiaries(String userId) {
        return beneficiariesRepository.findById(userId).orElseGet(() -> {
            UserBeneficiaries newUserBeneficiaries = new UserBeneficiaries();
            newUserBeneficiaries.setUserId(userId);
            newUserBeneficiaries.setBeneficiaries(new ArrayList<>());
            return newUserBeneficiaries;
        });
    }

    private UserBeneficiaryResponse mapToUserBeneficiaryResponse(UserBeneficiary beneficiary) {
        return UserBeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .name(beneficiary.getName())
                .email(beneficiary.getEmail())
                .build();
    }

    private ApiResponse<String> createSuccessResponse(String message) {
        return ApiResponse.<String>builder()
                .success(true)
                .data(message)
                .message(message)
                .build();
    }
}