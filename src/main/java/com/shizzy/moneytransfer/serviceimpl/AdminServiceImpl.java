package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.Admin;
import com.shizzy.moneytransfer.repository.AdminRepository;
import com.shizzy.moneytransfer.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;

    @Override
    public ApiResponse<List<Admin>> getAllAdmins()  {
        List<Admin> admins = adminRepository.findAll();
        if(admins.isEmpty()){
            throw new ResourceNotFoundException("No admin found");
        }
        return new ApiResponse<>(true, admins.size()+" Admins found", admins);
    }

}
