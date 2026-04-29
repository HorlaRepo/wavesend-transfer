package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.enums.EmailTemplateName;
import com.shizzy.moneytransfer.enums.TokenType;
import com.shizzy.moneytransfer.exception.OperationNotPermittedException;
import com.shizzy.moneytransfer.model.Token;
import com.shizzy.moneytransfer.model.User;
import com.shizzy.moneytransfer.repository.TokenRepository;
import com.shizzy.moneytransfer.repository.UserRepository;
import com.shizzy.moneytransfer.security.JwtTokenProvider;
import com.shizzy.moneytransfer.service.AuthService;
import com.shizzy.moneytransfer.service.EmailService;
import com.shizzy.moneytransfer.service.WalletService;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    private EmailService emailService;

    @Autowired
    @Qualifier("mailtrapEmailService")
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    @Value("${application.mailing.frontend.password-reset-url}")
    private String passwordResetUrl;

    @Override
    @Transactional
    public ApiResponse<User> registerUser(UserRegistrationRequestBody requestBody) throws MessagingException {
        // Check if email already exists
        if (userRepository.existsByEmail(requestBody.getEmail())) {
            throw new OperationNotPermittedException("Email already registered");
        }

        // Parse date of birth
        LocalDate dateOfBirth = LocalDate.parse(requestBody.getDateOfBirth(), DateTimeFormatter.ISO_LOCAL_DATE);

        // Create user
        User user = User.builder()
                .firstName(requestBody.getFirstName())
                .lastName(requestBody.getLastName())
                .email(requestBody.getEmail())
                .password(passwordEncoder.encode(requestBody.getPassword()))
                .phoneNumber(requestBody.getPhoneNumber())
                .dateOfBirth(dateOfBirth)
                .gender(requestBody.getGender())
                .enabled(false)
                .accountLocked(false)
                .roles("ROLE_USER")
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Create wallet for user
        try {
            walletService.createWallet(savedUser.getUserId().toString());
            log.info("Wallet created for user: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to create wallet for user: {}", savedUser.getEmail(), e);
            // Don't fail registration if wallet creation fails
        }

        // Generate verification token and send email
        sendVerificationEmail(savedUser);

        return ApiResponse.<User>builder()
                .success(true)
                .message("Registration successful. Please check your email to activate your account.")
                .data(savedUser)
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<String> registerAdmin(AdminRegistrationRequestBody requestBody) throws MessagingException {
        // Check if email already exists
        if (userRepository.existsByEmail(requestBody.getEmail())) {
            throw new OperationNotPermittedException("Email already registered");
        }

        // Create admin user
        User admin = User.builder()
                .firstName(requestBody.getFirstName())
                .lastName(requestBody.getLastName())
                .email(requestBody.getEmail())
                .password(passwordEncoder.encode(requestBody.getPassword()))
                .phoneNumber(null) // Admin doesn't have phone number in DTO
                .enabled(false)
                .accountLocked(false)
                .roles("ROLE_ADMIN")
                .build();

        User savedAdmin = userRepository.save(admin);
        log.info("Admin registered successfully: {}", savedAdmin.getEmail());

        // Generate verification token and send email
        sendVerificationEmail(savedAdmin);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Admin registration successful. Please check your email to activate your account.")
                .data(savedAdmin.getUserId().toString())
                .build();
    }

    @Override
    public ApiResponse<JwtResponseDTO> authenticateAndGetToken(AuthRequestDTO authRequestDTO) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequestDTO.getUsername(),
                            authRequestDTO.getPassword()
                    )
            );

            // Load user
            User user = userRepository.findByEmail(authRequestDTO.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Check if account is enabled
            if (!user.isEnabled()) {
                // Resend activation code
                try {
                    sendVerificationEmail(user);
                    log.info("Resent activation code to unactivated user: {}", user.getEmail());
                } catch (MessagingException e) {
                    log.error("Failed to resend activation code to: {}", user.getEmail(), e);
                }
                throw new DisabledException("Account not activated. A new activation code has been sent to your email.");
            }

            // Check if account is locked
            if (user.isAccountLocked()) {
                throw new LockedException("Account is locked. Please contact support.");
            }

            // Check if 2FA is enabled
            if (user.isTwoFactorEnabled()) {
                // Send 2FA code instead of returning token
                sendTwoFactorCode(user);

                JwtResponseDTO response = JwtResponseDTO.builder()
                        .accessToken(null)
                        .username(user.getEmail())
                        .twoFactorRequired(true)
                        .build();

                return ApiResponse.<JwtResponseDTO>builder()
                        .success(true)
                        .message("2FA code sent to your email")
                        .data(response)
                        .build();
            }

            // Generate JWT token
            String accessToken = jwtTokenProvider.generateAccessToken(user);

            JwtResponseDTO response = JwtResponseDTO.builder()
                    .accessToken(accessToken)
                    .username(user.getEmail())
                    .twoFactorRequired(false)
                    .build();

            log.info("User authenticated successfully: {}", user.getEmail());

            return ApiResponse.<JwtResponseDTO>builder()
                    .success(true)
                    .message("Authentication successful")
                    .data(response)
                    .build();

        } catch (BadCredentialsException e) {
            log.error("Bad credentials for user: {}", authRequestDTO.getUsername());
            throw new BadCredentialsException("Invalid email or password");
        } catch (DisabledException e) {
            log.error("Account disabled for user: {}", authRequestDTO.getUsername());
            // Return special response for unactivated accounts
            JwtResponseDTO response = JwtResponseDTO.builder()
                    .accessToken(null)
                    .username(authRequestDTO.getUsername())
                    .twoFactorRequired(false)
                    .activationRequired(true)
                    .build();

            return ApiResponse.<JwtResponseDTO>builder()
                    .success(false)
                    .message("Account not activated. A new activation code has been sent to your email.")
                    .data(response)
                    .build();
        } catch (LockedException e) {
            log.error("Account locked for user: {}", authRequestDTO.getUsername());
            throw new LockedException("Account is locked. Please contact support.");
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> activateAccount(String tokenString) throws MessagingException {
        Token token = tokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new OperationNotPermittedException("Invalid or expired token"));

        // Check if token is expired
        if (token.isExpired()) {
            // Send new verification email
            User user = userRepository.findByUserId(token.getUserId())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            sendVerificationEmail(user);
            throw new OperationNotPermittedException("Token expired. A new verification email has been sent.");
        }

        // Check if token is already validated
        if (token.isValidated()) {
            throw new OperationNotPermittedException("Token already used");
        }

        // Activate user account
        User user = userRepository.findByUserId(token.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setEnabled(true);
        userRepository.save(user);

        // Mark token as validated
        token.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(token);

        log.info("Account activated successfully: {}", user.getEmail());

        return ApiResponse.<String>builder()
                .success(true)
                .message("Account activated successfully. You can now login.")
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<String> requestPasswordReset(String email) throws MessagingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Generate password reset token
        String tokenString = generateVerificationCode(6);
        Token token = Token.builder()
                .token(tokenString)
                .tokenType(TokenType.PASSWORD_RESET)
                .userId(user.getUserId())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15)) // 15 minutes expiry
                .build();

        tokenRepository.save(token);
        log.info("Password reset token generated for user: {}", email);

        // Send password reset email
        sendPasswordResetEmail(user, tokenString);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Password reset instructions sent to your email")
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<String> resetPassword(String tokenString, String newPassword) {
        Token token = tokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new OperationNotPermittedException("Invalid or expired token"));

        // Check if token is expired
        if (token.isExpired()) {
            throw new OperationNotPermittedException("Token expired. Please request a new password reset.");
        }

        // Check if token is already validated
        if (token.isValidated()) {
            throw new OperationNotPermittedException("Token already used");
        }

        // Check if token type is PASSWORD_RESET
        if (token.getTokenType() != TokenType.PASSWORD_RESET) {
            throw new OperationNotPermittedException("Invalid token type");
        }

        // Reset password
        User user = userRepository.findByUserId(token.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as validated
        token.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(token);

        log.info("Password reset successfully for user: {}", user.getEmail());

        return ApiResponse.<String>builder()
                .success(true)
                .message("Password reset successfully. You can now login with your new password.")
                .build();
    }

    @Override
    public ApiResponse<String> changePassword(User user, ChangePasswordRequest request) {
        // Validate new password matches confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new OperationNotPermittedException("New password and confirmation do not match");
        }

        // Verify current password is correct
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Ensure new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new OperationNotPermittedException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getEmail());

        return ApiResponse.<String>builder()
                .success(true)
                .message("Password changed successfully")
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<String> toggleTwoFactor(User user, boolean enable) {
        user.setTwoFactorEnabled(enable);
        userRepository.save(user);

        String status = enable ? "enabled" : "disabled";
        log.info("Two-factor authentication {} for user: {}", status, user.getEmail());

        return ApiResponse.<String>builder()
                .success(true)
                .message("Two-factor authentication " + status + " successfully")
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<JwtResponseDTO> verifyTwoFactorCode(String username, String code) {
        Token token = tokenRepository.findByToken(code)
                .orElseThrow(() -> new OperationNotPermittedException("Invalid verification code"));

        if (token.isExpired()) {
            throw new OperationNotPermittedException("Verification code expired. Please login again.");
        }

        if (token.isValidated()) {
            throw new OperationNotPermittedException("Verification code already used");
        }

        if (token.getTokenType() != TokenType.TWO_FACTOR_AUTH) {
            throw new OperationNotPermittedException("Invalid token type");
        }

        // Find user and validate it matches
        User user = userRepository.findByUserId(token.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.getEmail().equals(username)) {
            throw new OperationNotPermittedException("Invalid verification code");
        }

        // Mark token as used
        token.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(token);

        // Generate JWT
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        JwtResponseDTO response = JwtResponseDTO.builder()
                .accessToken(accessToken)
                .username(user.getEmail())
                .twoFactorRequired(false)
                .build();

        log.info("2FA verified successfully for user: {}", user.getEmail());

        return ApiResponse.<JwtResponseDTO>builder()
                .success(true)
                .message("Authentication successful")
                .data(response)
                .build();
    }

    private void sendTwoFactorCode(User user) {
        String code = generateVerificationCode(6);
        Token token = Token.builder()
                .token(code)
                .tokenType(TokenType.TWO_FACTOR_AUTH)
                .userId(user.getUserId())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        tokenRepository.save(token);

        Map<String, Object> properties = new HashMap<>();
        properties.put("otp", code);
        properties.put("operation", "Login Verification");
        properties.put("validity", "10");
        properties.put("timestamp", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")));
        properties.put("current_year", String.valueOf(LocalDateTime.now().getYear()));

        try {
            emailService.sendEmail(
                    user.getEmail(),
                    properties,
                    EmailTemplateName.OTP_VERIFICATION,
                    "Login Verification Code"
            );
            log.info("2FA code sent to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send 2FA code to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send verification code");
        }
    }

    // Helper methods
    private void sendVerificationEmail(User user) throws MessagingException {
        // Invalidate any existing unvalidated activation tokens for this user
        tokenRepository.findAllByUserIdAndTokenType(user.getUserId(), TokenType.EMAIL_VERIFICATION)
                .stream()
                .filter(t -> t.getValidatedAt() == null && !t.isExpired())
                .forEach(t -> {
                    t.setExpiresAt(LocalDateTime.now()); // Mark as expired
                    tokenRepository.save(t);
                });

        String tokenString = generateVerificationCode(6);
        Token token = Token.builder()
                .token(tokenString)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .userId(user.getUserId())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24)) // 24 hours expiry
                .build();

        tokenRepository.save(token);

        // Send email - using the same template variables the OTP verification template expects
        Map<String, Object> properties = new HashMap<>();
        properties.put("otp", tokenString);
        properties.put("operation", "Account Activation");
        properties.put("validity", "1440"); // 24 hours in minutes
        properties.put("timestamp", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")));
        properties.put("current_year", String.valueOf(LocalDateTime.now().getYear()));

        emailService.sendEmail(
                user.getEmail(),
                properties,
                EmailTemplateName.OTP_VERIFICATION,
                "Account Activation - Your Verification Code"
        );

        log.info("Verification email sent to: {}", user.getEmail());
    }

    private void sendPasswordResetEmail(User user, String token) throws MessagingException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("otp", token);
        properties.put("operation", "Password Reset");
        properties.put("validity", "15"); // 15 minutes
        properties.put("timestamp", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")));
        properties.put("current_year", String.valueOf(LocalDateTime.now().getYear()));

        emailService.sendEmail(
                user.getEmail(),
                properties,
                EmailTemplateName.OTP_VERIFICATION,
                "Password Reset - Your Verification Code"
        );

        log.info("Password reset email sent to: {}", user.getEmail());
    }

    private String generateVerificationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }
}
