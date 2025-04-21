package com.shizzy.moneytransfer.serviceimpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.model.TransactionReference;
import com.shizzy.moneytransfer.repository.TransactionReferenceRepository;

@ExtendWith(MockitoExtension.class)
class TransactionReferenceServiceImplTest {

    @Mock
    private TransactionReferenceRepository transactionReferenceRepository;

    @InjectMocks
    private TransactionReferenceServiceImpl transactionReferenceService;

    private TransactionReference transactionReference;

    @BeforeEach
    void setUp() {
        transactionReference = TransactionReference.builder()
            .referenceNumber("ABCD1234EFGH5678")
            .build();
    }

    @Test
    void saveTransactionReference_WithSuffix_Successful() {
        String suffix = "XYZ";
        when(transactionReferenceRepository.save(any(TransactionReference.class)))
            .thenReturn(transactionReference);

        transactionReferenceService.saveTransactionReference(transactionReference, suffix);

        verify(transactionReferenceRepository, times(1))
            .save(argThat(tr -> tr.getReferenceNumber().endsWith(suffix)));
    }

    @Test
    void saveTransactionReference_NullSuffix_Successful() {
        when(transactionReferenceRepository.save(any(TransactionReference.class)))
            .thenReturn(transactionReference);

        transactionReferenceService.saveTransactionReference(transactionReference, null);

        verify(transactionReferenceRepository, times(1))
            .save(any(TransactionReference.class));
    }

    @Test
    void saveTransactionReference_WithReferenceNumber_Successful() {
        String referenceNumber = "TEST123456789012";
        when(transactionReferenceRepository.save(any(TransactionReference.class)))
            .thenReturn(transactionReference);

        transactionReferenceService.saveTransactionReference(referenceNumber);

        verify(transactionReferenceRepository, times(1))
            .save(argThat(tr -> tr.getReferenceNumber().equals(referenceNumber)));
    }

    @Test
    void getTransactionReferenceByReferenceNumber_Found_Successful() {
        String referenceNumber = "ABCD1234EFGH5678";
        when(transactionReferenceRepository.findByReferenceNumber(referenceNumber))
            .thenReturn(Optional.of(transactionReference));

        ApiResponse<TransactionReference> response = 
            transactionReferenceService.getTransactionReferenceByReferenceNumber(referenceNumber);

        assertTrue(response.isSuccess());
        assertEquals(transactionReference, response.getData());
        verify(transactionReferenceRepository, times(1))
            .findByReferenceNumber(referenceNumber);
    }

    @Test
    void getTransactionReferenceByReferenceNumber_NotFound_ThrowsException() {
        String referenceNumber = "NONEXISTENT12345";
        when(transactionReferenceRepository.findByReferenceNumber(referenceNumber))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            transactionReferenceService.getTransactionReferenceByReferenceNumber(referenceNumber));
        
        verify(transactionReferenceRepository, times(1))
            .findByReferenceNumber(referenceNumber);
    }

    @Test
    void generateUniqueReferenceNumber_NoDuplicates_ReturnsUnique() {
        String generatedRef = "ABCD1234EFGH5678";
        // First call returns false (no duplicate), so it uses the first generated number
        when(transactionReferenceRepository.existsByReferenceNumber(anyString()))
            .thenReturn(false);

        String result = transactionReferenceService.generateUniqueReferenceNumber();

        assertNotNull(result);
        assertEquals(16, result.length());
        verify(transactionReferenceRepository, atLeastOnce())
            .existsByReferenceNumber(anyString());
    }

    @Test
    void generateUniqueReferenceNumber_WithDuplicates_GeneratesUntilUnique() {
        // First two calls return true (duplicate exists), third returns false
        when(transactionReferenceRepository.existsByReferenceNumber(anyString()))
            .thenReturn(true)
            .thenReturn(true)
            .thenReturn(false);

        String result = transactionReferenceService.generateUniqueReferenceNumber();

        assertNotNull(result);
        assertEquals(16, result.length());
        verify(transactionReferenceRepository, times(3))
            .existsByReferenceNumber(anyString());
    }
}