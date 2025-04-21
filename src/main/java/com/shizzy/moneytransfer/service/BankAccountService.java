package com.shizzy.moneytransfer.service;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.AddBankAccountRequest;
import com.shizzy.moneytransfer.model.BankAccount;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface BankAccountService {
    ApiResponse<String> deleteBankAccount(Integer bankAccountId);
    ApiResponse<BankAccount> addBankAccount(Authentication connectedUser, AddBankAccountRequest addBankAccountRequest);
    ApiResponse<List<BankAccount>> getBankAccountsByUserId(Authentication connectedUser);
    ApiResponse<BankAccount> getBankAccountByAccountNumber(String accountNumber);
    ApiResponse<Long> getBankAccountCountByUserId(Authentication connectedUser);
}
