package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.dto.TransactionFee;
import com.shizzy.moneytransfer.serviceimpl.strategy.FeeCalculationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
class DefaultTransactionFeeServiceTest {

    @Mock
    private FeeCalculationStrategy feeCalculationStrategy;

    private DefaultTransactionFeeService transactionFeeService;

    @BeforeEach
    void setUp() {
        transactionFeeService = new DefaultTransactionFeeService(feeCalculationStrategy);
    }

    @Test
    void calculateFee_shouldDelegateToStrategyAndReturnResult() {
        // Arrange
        double amount = 100.0;
        BigDecimal expectedFee = BigDecimal.valueOf(5.0);
        when(feeCalculationStrategy.calculateFee(BigDecimal.valueOf(amount))).thenReturn(expectedFee);

        // Act
        TransactionFee result = transactionFeeService.calculateFee(amount);

        // Assert
        assertEquals(expectedFee.doubleValue(), result.getFee(), 0.001);
        verify(feeCalculationStrategy).calculateFee(BigDecimal.valueOf(amount));
    }

    @Test
    void calculateFee_withZeroAmount_shouldDelegateToStrategy() {
        // Arrange
        double amount = 0.0;
        BigDecimal expectedFee = BigDecimal.ZERO;
        when(feeCalculationStrategy.calculateFee(BigDecimal.valueOf(amount))).thenReturn(expectedFee);

        // Act
        TransactionFee result = transactionFeeService.calculateFee(amount);

        // Assert
        assertEquals(0.0, result.getFee(), 0.001);
        verify(feeCalculationStrategy).calculateFee(BigDecimal.valueOf(amount));
    }

    @Test
    void calculateFee_withLargeAmount_shouldDelegateToStrategy() {
        // Arrange
        double amount = 999999.99;
        BigDecimal expectedFee = BigDecimal.valueOf(9999.99);
        when(feeCalculationStrategy.calculateFee(BigDecimal.valueOf(amount))).thenReturn(expectedFee);

        // Act
        TransactionFee result = transactionFeeService.calculateFee(amount);

        // Assert
        assertEquals(expectedFee.doubleValue(), result.getFee(), 0.001);
        verify(feeCalculationStrategy).calculateFee(BigDecimal.valueOf(amount));
    }
}