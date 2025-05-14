package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.model.Admin;
import com.shizzy.moneytransfer.repository.AdminRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class Ping {

    private final AdminRepository adminRepository;
    
    public Ping(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @GetMapping("/ping")
    public String test() {
        try {
            return "Welcome";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

   
    @GetMapping("/currentAdminOrUser")
    public Object getCurrentAdminOrUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetail = (UserDetails) authentication.getPrincipal();
        String usernameFromAccessToken = userDetail.getUsername();

        Optional<Admin> admin = adminRepository.findAdminByUsername(usernameFromAccessToken);

        if (admin.isPresent()) {
            return admin.get();
        }
        return userDetail;
    }

    @GetMapping("/currentUser")
    public String getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String username = userDetails.getUsername();
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        return "Username: " + username + ", Role: " + role;
    }
}