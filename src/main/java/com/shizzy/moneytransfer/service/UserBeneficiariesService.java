package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.UserBeneficiaryRequest;
import com.shizzy.moneytransfer.dto.UserBeneficiaryResponse;
import com.shizzy.moneytransfer.dto.UserBeneficiariesResponse;
import org.springframework.security.core.Authentication;

public interface UserBeneficiariesService {
    ApiResponse<UserBeneficiaryResponse> addBeneficiary(Authentication connectedUser, UserBeneficiaryRequest beneficiaryRequest);
    ApiResponse<String> deleteBeneficiary(Authentication connectedUser, Long beneficiaryId);
    ApiResponse<UserBeneficiaryResponse> getBeneficiary(Authentication connectedUser, Long beneficiaryId);
    ApiResponse<UserBeneficiariesResponse> getBeneficiaries(Authentication connectedUser);
}