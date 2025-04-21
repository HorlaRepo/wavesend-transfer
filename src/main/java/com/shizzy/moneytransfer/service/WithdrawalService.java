package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.GenericResponse;
import com.shizzy.moneytransfer.dto.WithdrawalData;
import com.shizzy.moneytransfer.dto.WithdrawalInitiationResponse;
import com.shizzy.moneytransfer.dto.WithdrawalRequestMapper;
import com.shizzy.moneytransfer.dto.WithdrawalVerificationRequest;

public interface WithdrawalService {
    GenericResponse<WithdrawalData> withdraw(WithdrawalRequestMapper request);
    // New methods for OTP flow
    GenericResponse<WithdrawalInitiationResponse> initiateWithdrawal(WithdrawalRequestMapper request, String userId);
    GenericResponse<WithdrawalData> verifyAndWithdraw(WithdrawalVerificationRequest request, String userId);
}
