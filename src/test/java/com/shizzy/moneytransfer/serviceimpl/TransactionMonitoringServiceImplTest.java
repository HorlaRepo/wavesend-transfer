package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.exception.FraudulentTransactionException;
import com.shizzy.moneytransfer.model.FlaggedTransactionReason;
import com.shizzy.moneytransfer.model.Transaction;
import com.shizzy.moneytransfer.model.Wallet;
import com.shizzy.moneytransfer.repository.FlaggedTransactionReasonRepository;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.WalletService;
import com.shizzy.moneytransfer.service.detection.FraudDetectionRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionMonitoringServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FlaggedTransactionReasonRepository flaggedTransactionReasonRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private FraudDetectionRule rule1;

    @Mock
    private FraudDetectionRule rule2;

    @Mock
    private FraudDetectionRule rule3;

    private TransactionMonitoringServiceImpl transactionMonitoringService;

    private Transaction transaction;
    private Wallet wallet;
    private FlaggedTransactionReason reason1, reason2, reason3;

    @BeforeEach
    void setUp() {
        // Initialize fraud detection rules list
        List<FraudDetectionRule> rules = Arrays.asList(rule1, rule2, rule3);
        transactionMonitoringService = new TransactionMonitoringServiceImpl(
                transactionRepository,
                flaggedTransactionReasonRepository,
                walletService,
                rules);

        // Set up test wallet
        wallet = new Wallet();
        wallet.setWalletId(UUID.randomUUID().toString());

        // Set up test transaction
        transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setFlaggedTransactionReasons(new ArrayList<>());

        // Set up flagged transaction reasons
        reason1 = new FlaggedTransactionReason();
        reason2 = new FlaggedTransactionReason();
        reason3 = new FlaggedTransactionReason();
    }

    @Test
    void monitorTransaction_NoRulesTriggered_TransactionNotFlagged() {
        // Given
        when(rule1.evaluate(transaction)).thenReturn(false);
        when(rule2.evaluate(transaction)).thenReturn(false);
        when(rule3.evaluate(transaction)).thenReturn(false);

        // When
        transactionMonitoringService.monitorTransaction(transaction);

        // Then
        verify(rule1).evaluate(transaction);
        verify(rule2).evaluate(transaction);
        verify(rule3).evaluate(transaction);
        verify(flaggedTransactionReasonRepository, never()).saveAll(anyList());
        verify(transactionRepository, never()).save(any());
        verify(walletService, never()).flagWallet(any());
        assertFalse(transaction.isFlagged());
        assertTrue(transaction.getFlaggedTransactionReasons().isEmpty());
    }

    @Test
    void monitorTransaction_TwoRulesTriggered_TransactionFlagged() {
        // Given
        when(rule1.evaluate(transaction)).thenReturn(true);
        when(rule1.createReason(transaction)).thenReturn(reason1);
        when(rule2.evaluate(transaction)).thenReturn(true);
        when(rule2.createReason(transaction)).thenReturn(reason2);
        when(rule3.evaluate(transaction)).thenReturn(false);

        // When & Then
        assertThrows(
                FraudulentTransactionException.class,
                () -> transactionMonitoringService.monitorTransaction(transaction));

        verify(flaggedTransactionReasonRepository).saveAll(Arrays.asList(reason1, reason2));
        verify(transactionRepository).save(transaction);
        verify(walletService, never()).flagWallet(any());
        assertTrue(transaction.isFlagged());
        assertEquals(2, transaction.getFlaggedTransactionReasons().size());
    }

    @Test
    void monitorTransaction_ThreeRulesTriggered_TransactionAndWalletFlagged() {
        // Given
        when(rule1.evaluate(transaction)).thenReturn(true);
        when(rule1.createReason(transaction)).thenReturn(reason1);
        when(rule2.evaluate(transaction)).thenReturn(true);
        when(rule2.createReason(transaction)).thenReturn(reason2);
        when(rule3.evaluate(transaction)).thenReturn(true);
        when(rule3.createReason(transaction)).thenReturn(reason3);

        // When & Then
        assertThrows(
                FraudulentTransactionException.class,
                () -> transactionMonitoringService.monitorTransaction(transaction));

        verify(flaggedTransactionReasonRepository).saveAll(Arrays.asList(reason1, reason2, reason3));
        verify(transactionRepository).save(transaction);
        verify(walletService).flagWallet(wallet.getWalletId());
        assertTrue(transaction.isFlagged());
        assertEquals(3, transaction.getFlaggedTransactionReasons().size());
    }
}