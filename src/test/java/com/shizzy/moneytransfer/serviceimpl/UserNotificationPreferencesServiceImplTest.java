package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.UserNotificationPreferencesRequest;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.UserNotificationPreferences;
import com.shizzy.moneytransfer.repository.UserNotificationPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
class UserNotificationPreferencesServiceImplTest {

    @Mock
    private UserNotificationPreferencesRepository preferencesRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserNotificationPreferencesServiceImpl service;

    private static final String TEST_USER = "testUser";
    private UserNotificationPreferencesRequest request;
    private UserNotificationPreferences preferences;

    @BeforeEach
    void setUp() {
        lenient().when(authentication.getName()).thenReturn(TEST_USER);
        
        request = new UserNotificationPreferencesRequest(
                true, true, true, true, true
        );
        
        preferences = UserNotificationPreferences.builder()
                .createdBy(TEST_USER)
                .notifyOnDeposit(true)
                .notifyOnWithdraw(true)
                .notifyOnSend(true)
                .notifyOnReceive(true)
                .notifyOnPaymentFailure(true)
                .notifyOnScheduledTransfers(true)
                .notifyOnExecutedTransfers(true)
                .notifyOnCancelledTransfers(true)
                .build();
    }

    @Test
    void updateNotificationPreferences_WhenPreferencesExist_ShouldUpdateAndReturn() {
        // Arrange
        when(preferencesRepository.findByCreatedBy(TEST_USER)).thenReturn(Optional.of(preferences));
        when(preferencesRepository.save(any(UserNotificationPreferences.class))).thenReturn(preferences);

        // Act
        ApiResponse<UserNotificationPreferences> response = service.updateNotificationPreferences(authentication, request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Notification preferences updated successfully", response.getMessage());
        assertNotNull(response.getData());
        verify(preferencesRepository).findByCreatedBy(TEST_USER);
        verify(preferencesRepository).save(any(UserNotificationPreferences.class));
    }

    @Test
    void updateNotificationPreferences_WhenPreferencesDoNotExist_ShouldCreateAndReturn() {
        // Arrange
        when(preferencesRepository.findByCreatedBy(TEST_USER)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any(UserNotificationPreferences.class))).thenReturn(preferences);

        // Act
        ApiResponse<UserNotificationPreferences> response = service.updateNotificationPreferences(authentication, request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Notification preferences updated successfully", response.getMessage());
        assertNotNull(response.getData());
        verify(preferencesRepository).findByCreatedBy(TEST_USER);
        verify(preferencesRepository).save(any(UserNotificationPreferences.class));
    }

    @Test
    void getNotificationPreferences_WhenPreferencesExist_ShouldReturnPreferences() {
        // Arrange
        when(preferencesRepository.findByCreatedBy(TEST_USER)).thenReturn(Optional.of(preferences));

        // Act
        ApiResponse<UserNotificationPreferences> response = service.getNotificationPreferences(authentication);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Notification preferences retrieved successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(preferences, response.getData());
        verify(preferencesRepository).findByCreatedBy(TEST_USER);
    }

    @Test
    void getNotificationPreferences_WhenPreferencesDoNotExist_ShouldThrowException() {
        // Arrange
        when(preferencesRepository.findByCreatedBy(TEST_USER)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            service.getNotificationPreferences(authentication)
        );
        verify(preferencesRepository).findByCreatedBy(TEST_USER);
    }

    @Test
    void setDefNotificationPreferences_ShouldCreateDefaultPreferences() {
        // Arrange
        when(preferencesRepository.save(any(UserNotificationPreferences.class))).thenReturn(preferences);

        // Act
        service.setDefNotificationPreferences(TEST_USER);

        // Assert
        verify(preferencesRepository).save(any(UserNotificationPreferences.class));
    }
}