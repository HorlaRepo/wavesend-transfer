package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.Admin;

import java.util.List;

public interface AdminService {
    ApiResponse<List<Admin>> getAllAdmins();
}
