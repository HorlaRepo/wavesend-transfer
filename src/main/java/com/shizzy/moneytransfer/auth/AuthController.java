package com.shizzy.moneytransfer.auth;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.model.User;
import com.shizzy.moneytransfer.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("auth")
@Tag(name = "Authentication", description = "Authentication and registration endpoints")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.<User>builder()
                .success(true)
                .message("User profile retrieved")
                .data(user)
                .build());
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ApiResponse<JwtResponseDTO>> authenticateAndGetToken(
            @Valid @RequestBody AuthRequestDTO authRequestDTO) {
        return ResponseEntity.ok(authService.authenticateAndGetToken(authRequestDTO));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<User>> registerUser(
            @Valid @RequestBody UserRegistrationRequestBody requestBody) throws MessagingException {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(requestBody));
    }

    @PostMapping("/admin/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<String>> registerAdmin(
            @Valid @RequestBody AdminRegistrationRequestBody requestBody) throws MessagingException {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAdmin(requestBody));
    }

    @GetMapping("/activate-account")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ApiResponse<String>> activateAccount(@RequestParam String token) throws MessagingException {
        return ResponseEntity.ok(authService.activateAccount(token));
    }

    @PostMapping("/request-password-reset")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(@RequestParam String email) throws MessagingException {
        return ResponseEntity.ok(authService.requestPasswordReset(email));
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        return ResponseEntity.ok(authService.resetPassword(token, newPassword));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<String>builder().success(false).message("Authentication required").build());
        }
        return ResponseEntity.ok(authService.changePassword(user, request));
    }

    @PostMapping("/toggle-2fa")
    public ResponseEntity<ApiResponse<String>> toggleTwoFactor(
            @AuthenticationPrincipal User user,
            @RequestParam boolean enable) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<String>builder().success(false).message("Authentication required").build());
        }
        return ResponseEntity.ok(authService.toggleTwoFactor(user, enable));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<ApiResponse<JwtResponseDTO>> verifyTwoFactor(
            @RequestParam String username,
            @RequestParam String code) {
        return ResponseEntity.ok(authService.verifyTwoFactorCode(username, code));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponseDTO>> refreshToken(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
}
