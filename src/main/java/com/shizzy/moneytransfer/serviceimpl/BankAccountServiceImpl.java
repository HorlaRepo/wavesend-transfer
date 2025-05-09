package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.enums.Region;
import com.shizzy.moneytransfer.exception.DuplicateResourceException;
import com.shizzy.moneytransfer.exception.InvalidRequestException;
import com.shizzy.moneytransfer.service.BankAccountService;
import com.shizzy.moneytransfer.dto.AddBankAccountRequest;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.BankAccount;
import com.shizzy.moneytransfer.repository.BankAccountRepository;
import com.shizzy.moneytransfer.serviceimpl.builder.BankAccountBuilder;
import com.shizzy.moneytransfer.serviceimpl.factory.BankAccountBuilderFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final BankAccountBuilderFactory builderFactory;


    @Override
    public ApiResponse<String> deleteBankAccount(Integer bankAccountId) {
        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(()-> new ResourceNotFoundException("Bank Account not found"));

        bankAccountRepository.delete(bankAccount);

        return ApiResponse.<String>builder()
                .data("Bank Account deleted successfully")
                .message("Bank Account deleted successfully")
                .success(true)
                .build();
    }

    @Transactional
    @Override
    public ApiResponse<BankAccount> addBankAccount(Authentication connectedUser, AddBankAccountRequest request) {

        Region region = request.getRegion();
        Object bankAccountDetails = request.getBankAccountDetails();

        // Get the appropriate builder for the region
        BankAccountBuilder builder = builderFactory.getBuilder(region);

        try {
            // Build the BankAccount entity
            BankAccount bankAccount = builder.buildBankAccount(bankAccountDetails, connectedUser.getName());

            // Check for duplicates
            if (bankAccountRepository.findByAccountNumber(bankAccount.getAccountNumber()).isPresent()) {
                throw new DuplicateResourceException("Bank Account already exists");
            }

            // Save and return success response
            BankAccount savedAccount = bankAccountRepository.save(bankAccount);
            return ApiResponse.<BankAccount>builder()
                    .data(savedAccount)
                    .message("Bank Account added successfully")
                    .success(true)
                    .build();
        } catch (InvalidRequestException e) {
            // Handle invalid DTO or region errors
            throw new InvalidRequestException("Invalid Bank Account details for region: " + region);
        }
    }


    @Override
    public ApiResponse<List<BankAccount>> getBankAccountsByUserId(Authentication connectedUser) {

        List<BankAccount> bankAccounts = bankAccountRepository.findBankAccountByCreatedBy(connectedUser.getName());

        if (bankAccounts.isEmpty()){
            return ApiResponse.<List<BankAccount>>builder()
                    .data(null)
                    .message("No Bank Account found")
                    .success(false)
                    .build();
        }

        return ApiResponse.<List<BankAccount>>builder()
                .data(bankAccounts)
                .message("Bank Accounts retrieved successfully")
                .success(true)
                .build();
    }

    @Override
    public ApiResponse<BankAccount> getBankAccountByAccountNumber(String accountNumber) {
        BankAccount bankAccount = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new ResourceNotFoundException("Bank Account not found"));

        return ApiResponse.<BankAccount>builder()
                .data(bankAccount)
                .message("Bank Account retrieved successfully")
                .success(true)
                .build();
    }

    @Override
    public ApiResponse<Long> getBankAccountCountByUserId(Authentication connectedUser) {
        long count = bankAccountRepository.countBankAccountByCreatedBy(connectedUser.getName());

        return ApiResponse.<Long>builder()
                .data(count)
                .message("Bank Account count retrieved successfully")
                .success(true)
                .build();
    }
}
