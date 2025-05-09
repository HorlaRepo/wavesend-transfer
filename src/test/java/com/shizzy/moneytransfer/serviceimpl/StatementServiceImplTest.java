package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
public class StatementServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private StatementServiceImpl statementService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String userId;
    private Wallet wallet;
    private List<Transaction> transactions;

    @BeforeEach
    void setUp() {
        userId = "test-user";
        startDate = LocalDateTime.now().minusDays(30);
        endDate = LocalDateTime.now();
        
        wallet = new Wallet();
        wallet.setCreatedBy(userId);
        
        transactions = new ArrayList<>();
        // Add some test transactions
        Transaction transaction1 = new Transaction();
        transaction1.setTransactionDate(LocalDateTime.now().minusDays(15));
        transaction1.setDescription("Test Transaction 1");
        transaction1.setAmount(BigDecimal.valueOf(100.0));
        transaction1.setTransactionType(TransactionType.CREDIT);
        transaction1.setCurrentStatus("COMPLETED");
        
        Transaction transaction2 = new Transaction();
        transaction2.setTransactionDate(LocalDateTime.now().minusDays(10));
        transaction2.setDescription("Test Transaction 2");
        transaction2.setAmount(BigDecimal.valueOf(50.0));
        transaction2.setTransactionType(TransactionType.DEBIT);
        transaction2.setCurrentStatus("COMPLETED");
        
        transactions.add(transaction1);
        transactions.add(transaction2);
        
        // Set up common mock behavior
        lenient().when(authentication.getName()).thenReturn(userId);
    }

    @Test
    void generateStatement_whenUserIsNull_throwsIllegalArgumentException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            statementService.generateStatement(null, startDate, endDate, "pdf"));
    }

    @Test
    void generateStatement_whenWalletNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(walletRepository.findWalletByCreatedBy(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            statementService.generateStatement(authentication, startDate, endDate, "pdf"));
        
        verify(walletRepository).findWalletByCreatedBy(userId);
    }

    @Test
    void generateStatement_withInvalidFormat_returnsErrorResponse() throws IOException {
        // Arrange
        String invalidFormat = "xml";
        when(walletRepository.findWalletByCreatedBy(userId)).thenReturn(Optional.of(wallet));
        
        // Act
        ApiResponse<byte[]> response = statementService.generateStatement(authentication, startDate, endDate, invalidFormat);
        
        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Invalid format: " + invalidFormat + ". Use 'csv' or 'pdf'.", response.getMessage());
        assertNull(response.getData());
        
        verify(walletRepository).findWalletByCreatedBy(userId);
    }

    @Test
    void generateStatement_withCsvFormat_returnsSuccessResponse() throws IOException {
        // Arrange
        when(walletRepository.findWalletByCreatedBy(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findTransactionsByWalletAndTransactionDateBetween(wallet, startDate, endDate))
            .thenReturn(transactions);
        
        // Act
        ApiResponse<byte[]> response = statementService.generateStatement(authentication, startDate, endDate, "csv");
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Statement generated successfully", response.getMessage());
        assertNotNull(response.getData());
        assertTrue(response.getData().length > 0);
        
        verify(walletRepository).findWalletByCreatedBy(userId);
        verify(transactionRepository).findTransactionsByWalletAndTransactionDateBetween(wallet, startDate, endDate);
    }

    @Test
    void generateStatement_withPdfFormat_returnsSuccessResponse() throws IOException {
        // Arrange
        when(walletRepository.findWalletByCreatedBy(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findTransactionsByWalletAndTransactionDateBetween(wallet, startDate, endDate))
            .thenReturn(transactions);
        
        // Act
        ApiResponse<byte[]> response = statementService.generateStatement(authentication, startDate, endDate, "pdf");
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Statement generated successfully", response.getMessage());
        assertNotNull(response.getData());
        assertTrue(response.getData().length > 0);
        
        verify(walletRepository).findWalletByCreatedBy(userId);
        verify(transactionRepository).findTransactionsByWalletAndTransactionDateBetween(wallet, startDate, endDate);
    }

    @Test
    void generateStatement_whenExceptionOccurs_returnsErrorResponse() throws IOException {
        // Arrange
        when(walletRepository.findWalletByCreatedBy(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findTransactionsByWalletAndTransactionDateBetween(any(), any(), any()))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act
        ApiResponse<byte[]> response = statementService.generateStatement(authentication, startDate, endDate, "pdf");
        
        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Error generating statement: Database error", response.getMessage());
        assertNull(response.getData());
        
        verify(walletRepository).findWalletByCreatedBy(userId);
        verify(transactionRepository).findTransactionsByWalletAndTransactionDateBetween(wallet, startDate, endDate);
    }
}