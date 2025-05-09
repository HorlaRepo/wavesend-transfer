package com.shizzy.moneytransfer.serviceimpl;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shizzy.moneytransfer.dto.CreateTransactionRequestBody;
import com.shizzy.moneytransfer.kafka.NotificationProducer;
import com.shizzy.moneytransfer.repository.RefundImpactRecordRepository;
import com.shizzy.moneytransfer.repository.TransactionRepository;
import com.shizzy.moneytransfer.service.AccountLimitService;
import com.shizzy.moneytransfer.service.KeycloakService;
import com.shizzy.moneytransfer.service.OtpService;
import com.shizzy.moneytransfer.service.TransactionLimitService;
import com.shizzy.moneytransfer.service.TransactionReferenceService;
import com.shizzy.moneytransfer.service.TransactionService;
import com.shizzy.moneytransfer.service.WalletService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;





@ExtendWith(MockitoExtension.class)
public class MoneyTransferServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private TransactionReferenceService referenceService;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private NotificationProducer notificationProducer;
    @Mock
    private TransactionService transactionService;
    @Mock
    private OtpService otpService;
    @Mock
    private TransactionLimitService transactionLimitService;
    @Mock
    private AccountLimitService accountLimitService;
    @Mock
    private RefundImpactRecordRepository refundImpactRecordRepository;

    @InjectMocks
    private MoneyTransferServiceImpl moneyTransferService;

    private CreateTransactionRequestBody requestBody;
    private Class<?> pendingTransferClass;

    @BeforeEach
    void setUp() throws Exception {
        requestBody = new CreateTransactionRequestBody(
                "sender@example.com",
                "receiver@example.com",
                BigDecimal.valueOf(100.00),
                "Payment for services"
        );

        // Access the PendingTransfer inner class using reflection
        pendingTransferClass = Class.forName("com.shizzy.moneytransfer.serviceimpl.MoneyTransferServiceImpl$PendingTransfer");
    }

    @Test
    void pendingTransfer_ShouldStoreRequestBody() throws Exception {
        // Create a new PendingTransfer instance using reflection
        Constructor<?> constructor = pendingTransferClass.getDeclaredConstructor(CreateTransactionRequestBody.class);
        constructor.setAccessible(true);
        Object pendingTransfer = constructor.newInstance(requestBody);

        // Access the getRequestBody method
        Method getRequestBodyMethod = pendingTransferClass.getDeclaredMethod("getRequestBody");
        getRequestBodyMethod.setAccessible(true);
        CreateTransactionRequestBody storedRequestBody = 
                (CreateTransactionRequestBody) getRequestBodyMethod.invoke(pendingTransfer);

        // Verify the request body was stored correctly
        assertEquals(requestBody, storedRequestBody);
    }

    @Test
    void pendingTransfer_ShouldSetCreatedAtToCurrentTime() throws Exception {
        // Capture current time before creating PendingTransfer
        LocalDateTime beforeCreation = LocalDateTime.now();

        // Create a new PendingTransfer instance
        Constructor<?> constructor = pendingTransferClass.getDeclaredConstructor(CreateTransactionRequestBody.class);
        constructor.setAccessible(true);
        Object pendingTransfer = constructor.newInstance(requestBody);
        
        // Get creation time
        Method getCreatedAtMethod = pendingTransferClass.getDeclaredMethod("getCreatedAt");
        getCreatedAtMethod.setAccessible(true);
        LocalDateTime createdAt = (LocalDateTime) getCreatedAtMethod.invoke(pendingTransfer);
        
        // Capture time after creation
        LocalDateTime afterCreation = LocalDateTime.now();
        
        // Verify creation time is between before and after
        assertNotNull(createdAt);
        assertFalse(createdAt.isBefore(beforeCreation));
        assertFalse(createdAt.isAfter(afterCreation));
    }

    @Test
    void isExpired_ShouldReturnFalseForNewTransfer() throws Exception {
        // Create a new PendingTransfer instance
        Constructor<?> constructor = pendingTransferClass.getDeclaredConstructor(CreateTransactionRequestBody.class);
        constructor.setAccessible(true);
        Object pendingTransfer = constructor.newInstance(requestBody);
        
        // Check if expired
        Method isExpiredMethod = pendingTransferClass.getDeclaredMethod("isExpired");
        isExpiredMethod.setAccessible(true);
        boolean isExpired = (boolean) isExpiredMethod.invoke(pendingTransfer);
        
        // New transfer should not be expired
        assertFalse(isExpired);
    }

    @Test
    void isExpired_ShouldReturnTrueForOldTransfer() throws Exception {
        // Create a new PendingTransfer instance
        Constructor<?> constructor = pendingTransferClass.getDeclaredConstructor(CreateTransactionRequestBody.class);
        constructor.setAccessible(true);
        Object pendingTransfer = constructor.newInstance(requestBody);
        
        // Modify the createdAt field to make it older
        Field createdAtField = pendingTransferClass.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(pendingTransfer, LocalDateTime.now().minus(Duration.ofMinutes(16))); // Expired (> 15 minutes)
        
        // Check if expired
        Method isExpiredMethod = pendingTransferClass.getDeclaredMethod("isExpired");
        isExpiredMethod.setAccessible(true);
        boolean isExpired = (boolean) isExpiredMethod.invoke(pendingTransfer);
        
        // Old transfer should be expired
        assertTrue(isExpired);
    }
}