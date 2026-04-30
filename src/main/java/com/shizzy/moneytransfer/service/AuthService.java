package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.model.User;
import jakarta.mail.MessagingException;
import lombok.NonNull;

public interface AuthService {
    ApiResponse<User> registerUser(@NonNull UserRegistrationRequestBody requestBody) throws MessagingException;
    ApiResponse<String> registerAdmin(AdminRegistrationRequestBody requestBody) throws MessagingException;
    ApiResponse<JwtResponseDTO> authenticateAndGetToken(AuthRequestDTO authRequestDTO);
    ApiResponse<String> activateAccount(String token) throws MessagingException;
    ApiResponse<String> requestPasswordReset(String email) throws MessagingException;
    ApiResponse<String> resetPassword(String token, String newPassword);
    ApiResponse<String> changePassword(User user, ChangePasswordRequest request);
    ApiResponse<String> toggleTwoFactor(User user, boolean enable);
    ApiResponse<JwtResponseDTO> verifyTwoFactorCode(String username, String code);
    ApiResponse<JwtResponseDTO> refreshToken(String refreshToken);
}
