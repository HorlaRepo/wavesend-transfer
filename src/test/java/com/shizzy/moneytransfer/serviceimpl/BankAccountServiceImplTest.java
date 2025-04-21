package com.shizzy.moneytransfer.serviceimpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.AddBankAccountRequest;
import com.shizzy.moneytransfer.enums.Region;
import com.shizzy.moneytransfer.exception.DuplicateResourceException;
import com.shizzy.moneytransfer.exception.InvalidRequestException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.BankAccount;
import com.shizzy.moneytransfer.repository.BankAccountRepository;
import com.shizzy.moneytransfer.serviceimpl.builder.BankAccountBuilder;
import com.shizzy.moneytransfer.serviceimpl.factory.BankAccountBuilderFactory;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceImplTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private BankAccountBuilderFactory builderFactory;

    @Mock
    private BankAccountBuilder bankAccountBuilder;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BankAccountServiceImpl bankAccountService;

    private BankAccount bankAccount;
    private AddBankAccountRequest request;

    @BeforeEach
    void setUp() {

        bankAccount = BankAccount.builder()
                .id(1L)
                .accountNumber("1234567890")
                .createdBy("testUser")
                .region("AFRICA")
                .build();

        request = new AddBankAccountRequest(Region.AFRICA, new Object());
    }

    @Test
    void deleteBankAccount_Successful() {
        when(bankAccountRepository.findById(1)).thenReturn(Optional.of(bankAccount));

        ApiResponse<String> response = bankAccountService.deleteBankAccount(1);

        assertTrue(response.isSuccess());
        assertEquals("Bank Account deleted successfully", response.getData());
        verify(bankAccountRepository, times(1)).delete(bankAccount);
    }

    @Test
    void deleteBankAccount_NotFound_ThrowsException() {
        when(bankAccountRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bankAccountService.deleteBankAccount(1));
    }

    @Test
    void addBankAccount_Successful() {
        // Change the stubbing to match the Region.AFRICA that's in your request object
        when(builderFactory.getBuilder(Region.AFRICA)).thenReturn(bankAccountBuilder);
        when(bankAccountBuilder.buildBankAccount(any(), eq("testUser"))).thenReturn(bankAccount);
        when(bankAccountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(bankAccount);

        // Add authentication stubbing
        when(authentication.getName()).thenReturn("testUser");

        ApiResponse<BankAccount> response = bankAccountService.addBankAccount(authentication, request);

        assertTrue(response.isSuccess());
        assertEquals(bankAccount, response.getData());
        assertEquals("Bank Account added successfully", response.getMessage());
        verify(bankAccountRepository, times(1)).save(bankAccount);
    }

    @Test
    void addBankAccount_Duplicate_ThrowsException() {
        // Fix: Change Region.US to Region.AFRICA to match the request object
        when(builderFactory.getBuilder(Region.AFRICA)).thenReturn(bankAccountBuilder);
        when(bankAccountBuilder.buildBankAccount(any(), eq("testUser"))).thenReturn(bankAccount);
        when(bankAccountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.of(bankAccount));

        // Add authentication stubbing
        when(authentication.getName()).thenReturn("testUser");

        assertThrows(DuplicateResourceException.class,
                () -> bankAccountService.addBankAccount(authentication, request));
    }

    @Test
    void addBankAccount_InvalidDetails_ThrowsException() {
        // Fix: Change Region.US to Region.AFRICA to match the request object
        when(builderFactory.getBuilder(Region.AFRICA)).thenReturn(bankAccountBuilder);
        when(bankAccountBuilder.buildBankAccount(any(), eq("testUser")))
                .thenThrow(new InvalidRequestException("Invalid details"));

        // Add authentication stubbing
        when(authentication.getName()).thenReturn("testUser");

        assertThrows(InvalidRequestException.class,
                () -> bankAccountService.addBankAccount(authentication, request));
    }

    @Test
    void getBankAccountsByUserId_Found_Successful() {
        List<BankAccount> accounts = Collections.singletonList(bankAccount);
        when(authentication.getName()).thenReturn("testUser");
        when(bankAccountRepository.findBankAccountByCreatedBy("testUser")).thenReturn(accounts);

        ApiResponse<List<BankAccount>> response = bankAccountService.getBankAccountsByUserId(authentication);

        assertTrue(response.isSuccess());
        assertEquals(accounts, response.getData());
        assertEquals("Bank Accounts retrieved successfully", response.getMessage());
    }

    @Test
    void getBankAccountsByUserId_NotFound_ReturnsEmpty() {
        when(authentication.getName()).thenReturn("testUser");
        when(bankAccountRepository.findBankAccountByCreatedBy("testUser")).thenReturn(Collections.emptyList());

        ApiResponse<List<BankAccount>> response = bankAccountService.getBankAccountsByUserId(authentication);

        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertEquals("No Bank Account found", response.getMessage());
    }

    @Test
    void getBankAccountByAccountNumber_Found_Successful() {
        when(bankAccountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.of(bankAccount));

        ApiResponse<BankAccount> response = bankAccountService.getBankAccountByAccountNumber("1234567890");

        assertTrue(response.isSuccess());
        assertEquals(bankAccount, response.getData());
        assertEquals("Bank Account retrieved successfully", response.getMessage());
    }

    @Test
    void getBankAccountByAccountNumber_NotFound_ThrowsException() {
        when(bankAccountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bankAccountService.getBankAccountByAccountNumber("1234567890"));
    }

    @Test
    void getBankAccountCountByUserId_Successful() {
        when(authentication.getName()).thenReturn("testUser");
        when(bankAccountRepository.countBankAccountByCreatedBy("testUser")).thenReturn(5L);

        ApiResponse<Long> response = bankAccountService.getBankAccountCountByUserId(authentication);

        assertTrue(response.isSuccess());
        assertEquals(5L, response.getData());
        assertEquals("Bank Account count retrieved successfully", response.getMessage());
    }

    @Test
    void getBankAccountCountByUserId_ZeroCount_Successful() {
        when(authentication.getName()).thenReturn("testUser");
        when(bankAccountRepository.countBankAccountByCreatedBy("testUser")).thenReturn(0L);

        ApiResponse<Long> response = bankAccountService.getBankAccountCountByUserId(authentication);

        assertTrue(response.isSuccess());
        assertEquals(0L, response.getData());
        assertEquals("Bank Account count retrieved successfully", response.getMessage());
    }
}