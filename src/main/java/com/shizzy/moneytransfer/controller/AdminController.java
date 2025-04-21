package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.Admin;
import com.shizzy.moneytransfer.service.AdminService;
import com.shizzy.moneytransfer.serviceimpl.AdminServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/admin")
    public ApiResponse<List<Admin>> getAllAdmins() {
        return adminService.getAllAdmins();
    }

}
