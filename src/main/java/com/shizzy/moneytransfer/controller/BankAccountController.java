package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.AddBankAccountRequest;
import com.shizzy.moneytransfer.dto.NigerianWithdrawalRequest;
import com.shizzy.moneytransfer.model.BankAccount;
import com.shizzy.moneytransfer.service.BankAccountService;
import com.shizzy.moneytransfer.serviceimpl.BankAccountServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("bank-account")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping
    private ApiResponse<BankAccount> addBankAccount(Authentication connectedUser,  @RequestBody @Valid AddBankAccountRequest addBankAccountRequest){
        return bankAccountService.addBankAccount(connectedUser, addBankAccountRequest);
    }

    @GetMapping
    public ApiResponse<List<BankAccount>> getBankAccountsByUser(Authentication connectedUser) {
        return bankAccountService.getBankAccountsByUserId(connectedUser);
    }

    @GetMapping("/{accountNumber}")
    public ApiResponse<BankAccount> getBankAccountByAccountNumber(@PathVariable String accountNumber){
        return bankAccountService.getBankAccountByAccountNumber(accountNumber);
    }

    @GetMapping("/count")
    public ApiResponse<Long> getBankAccountCountByUserId(Authentication connectedUser){
        return bankAccountService.getBankAccountCountByUserId(connectedUser);
    }

    @DeleteMapping("/{bankAccountId}")
    public ApiResponse<String> deleteBankAccount(@PathVariable Integer bankAccountId){
        return bankAccountService.deleteBankAccount(bankAccountId);
    }
}
