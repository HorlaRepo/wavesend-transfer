//package com.shizzy.moneytransfer.auth;
//
//import com.shizzy.moneytransfer.api.ApiResponse;
//import com.shizzy.moneytransfer.dto.AdminRegistrationRequestBody;
//import com.shizzy.moneytransfer.dto.AuthRequestDTO;
//import com.shizzy.moneytransfer.dto.JwtResponseDTO;
//import com.shizzy.moneytransfer.dto.UserRegistrationRequestBody;
//import com.shizzy.moneytransfer.model.User;
//import com.shizzy.moneytransfer.security.jwt.JwtService;
//import com.shizzy.moneytransfer.serviceimpl.AdminServiceImpl;
//import jakarta.mail.MessagingException;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.web.bind.annotation.*;
//
////@RestController
//@RequiredArgsConstructor
////@RequestMapping("auth")
//public class AuthController {
//
//    private final AuthenticationManager authenticationManager;
//    private final JwtService jwtService;
//    private final AdminServiceImpl adminServiceImpl;
//    private final AuthServiceImpl authServiceImpl;
//
//    @PostMapping("/login")
//    public ApiResponse<JwtResponseDTO> authenticateAndGetToken(@Valid @RequestBody AuthRequestDTO authRequestDTO) {
//        return authServiceImpl.authenticateAndGetToken(authRequestDTO);
//    }
//
//    @PostMapping("/admin/register")
//    public ApiResponse<String> addAdmin(@Valid @RequestBody AdminRegistrationRequestBody requestBody) throws MessagingException {
//        return authServiceImpl.registerAdmin(requestBody);
//    }
//
//    @PostMapping("/register")
//    public ApiResponse<User> registerUser(@Valid @RequestBody UserRegistrationRequestBody requestBody) throws MessagingException {
//        return authServiceImpl.registerUser(requestBody);
//    }
//
//    @GetMapping("/activate-account")
//    public ApiResponse<String> activateAccount(@RequestParam String token) throws MessagingException {
//        return authServiceImpl.activateAccount(token);
//    }
//
//}
