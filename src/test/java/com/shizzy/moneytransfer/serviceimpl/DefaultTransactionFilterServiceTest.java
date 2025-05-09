package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
class DefaultTransactionFilterServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DefaultTransactionFilterService transactionFilterService;

    private Long walletId;
    private int pageNumber;
    private int pageSize;
    private PageRequest pageRequest;
    private Page<Transaction> transactionPage;

    @BeforeEach
    void setUp() {
        walletId = 1L;
        pageNumber = 0;
        pageSize = 10;
        pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "transactionId"));
        
        Transaction transaction = new Transaction();
        transaction.setTransactionId(1);
        List<Transaction> transactionList = Collections.singletonList(transaction);
        transactionPage = new PageImpl<>(transactionList, pageRequest, 1);
    }

    @Test
    void getTransactionByFilter_WithDateRange_PaymentsSent() {
        // Given
        String filter = "PAYMENTS_SENT";
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        when(transactionRepository.findByWalletIdAndOperationAndTransactionTypeAndTransactionDateBetween(
                eq(walletId), eq(TransactionOperation.TRANSFER), eq(TransactionType.DEBIT), 
                eq(startDateTime), eq(endDateTime), any(PageRequest.class)))
            .thenReturn(transactionPage);

        // When
        ApiResponse<Page<Transaction>> result = transactionFilterService.getTransactionByFilter(
                walletId, filter, Optional.of(startDate), Optional.of(endDate), pageNumber, pageSize);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("1 transactions found", result.getMessage());
        assertEquals(transactionPage, result.getData());
        verify(transactionRepository).findByWalletIdAndOperationAndTransactionTypeAndTransactionDateBetween(
                eq(walletId), eq(TransactionOperation.TRANSFER), eq(TransactionType.DEBIT), 
                eq(startDateTime), eq(endDateTime), any(PageRequest.class));
    }
    
    @Test
    void getTransactionByFilter_WithDateRange_PaymentsReceived() {
        // Given
        String filter = "PAYMENTS_RECEIVED";
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        when(transactionRepository.findByWalletIdAndOperationAndTransactionTypeAndTransactionDateBetween(
                eq(walletId), eq(TransactionOperation.TRANSFER), eq(TransactionType.CREDIT), 
                any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
            .thenReturn(transactionPage);

        // When
        ApiResponse<Page<Transaction>> result = transactionFilterService.getTransactionByFilter(
                walletId, filter, Optional.of(startDate), Optional.of(endDate), pageNumber, pageSize);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(transactionPage, result.getData());
    }
    
    @Test
    void getTransactionByFilter_WithDateRange_Refunds() {
        // Given
        String filter = "REFUNDS";
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        when(transactionRepository.findByWalletIdAndOperationAndTransactionDateBetween(
                eq(walletId), eq(TransactionOperation.REVERSAL), 
                any(LocalDateTime.class), any(LocalDateTime.class), any(PageRequest.class)))
            .thenReturn(transactionPage);

        // When
        ApiResponse<Page<Transaction>> result = transactionFilterService.getTransactionByFilter(
                walletId, filter, Optional.of(startDate), Optional.of(endDate), pageNumber, pageSize);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(transactionPage, result.getData());
    }
    
    @Test
    void getTransactionByFilter_WithoutDateRange_Withdrawal() {
        // Given
        String filter = "WITHDRAWAL";
        
        when(transactionRepository.findByWalletIdAndOperation(
                eq(walletId), eq(TransactionOperation.WITHDRAWAL), any(PageRequest.class)))
            .thenReturn(transactionPage);

        // When
        ApiResponse<Page<Transaction>> result = transactionFilterService.getTransactionByFilter(
                walletId, filter, Optional.empty(), Optional.empty(), pageNumber, pageSize);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(transactionPage, result.getData());
    }
    
    @Test
    void getTransactionByFilter_WithoutDateRange_Deposit() {
        // Given
        String filter = "DEPOSIT";
        
        when(transactionRepository.findByWalletIdAndOperation(
                eq(walletId), eq(TransactionOperation.DEPOSIT), any(PageRequest.class)))
            .thenReturn(transactionPage);

        // When
        ApiResponse<Page<Transaction>> result = transactionFilterService.getTransactionByFilter(
                walletId, filter, Optional.empty(), Optional.empty(), pageNumber, pageSize);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("1 transactions found", result.getMessage());
        assertEquals(transactionPage, result.getData());
    }
    
    @Test
    void getTransactionByFilter_WithoutDateRange_PaymentsSent() {
        // Given
        String filter = "PAYMENTS_SENT";
        
        when(transactionRepository.findByWalletIdAndOperationAndTransactionType(
                eq(walletId), eq(TransactionOperation.TRANSFER), eq(TransactionType.DEBIT), any(PageRequest.class)))
            .thenReturn(transactionPage);

        // When
        ApiResponse<Page<Transaction>> result = transactionFilterService.getTransactionByFilter(
                walletId, filter, Optional.empty(), Optional.empty(), pageNumber, pageSize);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(transactionPage, result.getData());
    }
    
    @Test
    void getTransactionByFilter_InvalidFilter() {
        // Given
        String filter = "INVALID_FILTER";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            transactionFilterService.getTransactionByFilter(
                walletId, filter, Optional.empty(), Optional.empty(), pageNumber, pageSize)
        );
    }
    
    @Test
    void getTransactionByFilter_Pagination() {
        // Given
        String filter = "DEPOSIT";
        int differentPageNumber = 1;
        int differentPageSize = 5;
        PageRequest differentPageRequest = PageRequest.of(differentPageNumber, differentPageSize, 
            Sort.by(Sort.Direction.DESC, "transactionId"));
        
        when(transactionRepository.findByWalletIdAndOperation(
                eq(walletId), eq(TransactionOperation.DEPOSIT), eq(differentPageRequest)))
            .thenReturn(transactionPage);

        // When
        ApiResponse<Page<Transaction>> result = transactionFilterService.getTransactionByFilter(
                walletId, filter, Optional.empty(), Optional.empty(), differentPageNumber, differentPageSize);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(transactionPage, result.getData());
        verify(transactionRepository).findByWalletIdAndOperation(
                eq(walletId), eq(TransactionOperation.DEPOSIT), eq(differentPageRequest));
    }
}