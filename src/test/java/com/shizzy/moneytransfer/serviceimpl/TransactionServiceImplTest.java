package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.*;
import com.shizzy.moneytransfer.enums.RefundStatus;
import com.shizzy.moneytransfer.enums.TransactionOperation;
import com.shizzy.moneytransfer.enums.TransactionStatus;
import com.shizzy.moneytransfer.enums.TransactionType;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.*;
import com.shizzy.moneytransfer.repository.*;
import com.shizzy.moneytransfer.service.TransactionFeeService;
import com.shizzy.moneytransfer.service.TransactionFilterService;
import com.shizzy.moneytransfer.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private TransactionStatusRepository statusRepository;
    
    @Mock
    private WalletRepository walletRepository;
    
    @Mock
    private TransactionFeeService transactionFeeService;
    
    @Mock
    private TransactionFilterService transactionFilterService;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private TransactionReferenceRepository referenceRepository;
    
    @Mock
    private WalletService walletService;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Transaction transaction;
    private Wallet wallet;
    
    @BeforeEach
    void setUp() {
        wallet = Wallet.builder()
                .walletId("WALLET123")
                .balance(BigDecimal.valueOf(1000.0))
                .build();
        
        transaction = Transaction.builder()
                .transactionId(1)
                .wallet(wallet)
                .amount(BigDecimal.valueOf(100.0))
                .transactionType(TransactionType.CREDIT)
                .operation(TransactionOperation.DEPOSIT)
                .description("Test transaction")
                .currentStatus(TransactionStatus.SUCCESS.getValue())
                .referenceNumber("REF123")
                .transactionDate(LocalDateTime.now())
                .refundableAmount(BigDecimal.valueOf(100.0))
                .refundStatus(RefundStatus.FULLY_REFUNDABLE)
                .build();
    }

    @Test
    void getAllTransactions_Success() {
        // Arrange
        List<Transaction> transactions = List.of(transaction);
        Page<Transaction> transactionPage = new PageImpl<>(transactions);
        when(transactionRepository.findAll(any(PageRequest.class))).thenReturn(transactionPage);

        // Act
        ApiResponse<PagedTransactionResponse> response = transactionService.getAllTransactions(0, 10);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().getContent().size());
        assertEquals("1 transactions found", response.getMessage());
        verify(transactionRepository).findAll(any(PageRequest.class));
    }

    @Test
    void getAllTransactions_ThrowsException_WhenNoTransactionsFound() {
        // Arrange
        Page<Transaction> emptyPage = new PageImpl<>(new ArrayList<>());
        when(transactionRepository.findAll(any(PageRequest.class))).thenReturn(emptyPage);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.getAllTransactions(0, 10));
        verify(transactionRepository).findAll(any(PageRequest.class));
    }

    @Test
    void getTransactionByReferenceNumber_Success() {
        // Arrange
        List<Transaction> transactions = List.of(transaction);
        when(transactionRepository.findTransactionByReferenceNumber("REF123")).thenReturn(transactions);

        // Act
        ApiResponse<List<TransactionResponse>> response = transactionService.getTransactionByReferenceNumber("REF123");

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("1 transactions found", response.getMessage());
        verify(transactionRepository).findTransactionByReferenceNumber("REF123");
    }

    @Test
    void getTransactionById_Success() {
        // Arrange
        when(transactionRepository.findById(1)).thenReturn(Optional.of(transaction));

        // Act
        ApiResponse<TransactionResponse> response = transactionService.getTransactionById(1);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Transaction found", response.getMessage());
        assertNotNull(response.getData());
        verify(transactionRepository).findById(1);
    }

    @Test
    void getTransactionById_ThrowsException_WhenNotFound() {
        // Arrange
        when(transactionRepository.findById(1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transactionService.getTransactionById(1));
        verify(transactionRepository).findById(1);
    }

    @Test
    void completeDeposit_Success() {
        // Act
        transactionService.completeDeposit(transaction, "SESSION123", "PROVIDER123", 
                BigDecimal.valueOf(100.0), RefundStatus.FULLY_REFUNDABLE);

        // Assert
        assertEquals(TransactionStatus.SUCCESS.getValue(), transaction.getCurrentStatus());
        assertEquals("SESSION123", transaction.getSessionId());
        assertEquals("PROVIDER123", transaction.getProviderId());
        verify(transactionRepository).save(transaction);
        verify(walletService).deposit(any(Wallet.class), any(BigDecimal.class));
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void createReversalTransaction_Success() {
        // Arrange
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        // Act
        Transaction result = transactionService.createReversalTransaction(wallet, 
                BigDecimal.valueOf(100.0), "Reversal", TransactionOperation.REVERSAL);

        // Assert
        assertNotNull(result);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void getTransactionFee_Success() {
        // Arrange
        TransactionFee fee = new TransactionFee();
        fee.setFee(10.0);
        when(transactionFeeService.calculateFee(100.0)).thenReturn(fee);

        // Act
        ApiResponse<TransactionFee> response = transactionService.getTransactionFee(100.0);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Transaction fee calculated successfully", response.getMessage());
        assertEquals(10.0, response.getData().getFee());
        verify(transactionFeeService).calculateFee(100.0);
    }
}