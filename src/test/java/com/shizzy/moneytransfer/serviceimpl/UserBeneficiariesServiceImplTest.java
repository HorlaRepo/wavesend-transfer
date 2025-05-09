package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.dto.UserBeneficiaryRequest;
import com.shizzy.moneytransfer.dto.UserBeneficiaryResponse;
import com.shizzy.moneytransfer.dto.UserBeneficiariesResponse;
import com.shizzy.moneytransfer.exception.DuplicateResourceException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.UserBeneficiary;
import com.shizzy.moneytransfer.model.UserBeneficiaries;
import com.shizzy.moneytransfer.repository.UserBeneficiariesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
public class UserBeneficiariesServiceImplTest {

    @Mock
    private UserBeneficiariesRepository beneficiariesRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserBeneficiariesServiceImpl userBeneficiariesService;

    private UserBeneficiaries userBeneficiaries;
    private UserBeneficiary beneficiary;
    private final String USERNAME = "testUser";
    private final Long BENEFICIARY_ID = 1L;

    @BeforeEach
    void setUp() {
        when(authentication.getName()).thenReturn(USERNAME);

        beneficiary = UserBeneficiary.builder()
                .id(BENEFICIARY_ID)
                .name("Test Beneficiary")
                .email("test@example.com")
                .build();

        userBeneficiaries = new UserBeneficiaries();
        userBeneficiaries.setUserId(USERNAME);
        userBeneficiaries.setBeneficiaries(new ArrayList<>(Arrays.asList(beneficiary)));
    }

    @Test
    void addBeneficiary_Success() {
        // Arrange
        UserBeneficiaryRequest request = new UserBeneficiaryRequest();
        request.setName("New Beneficiary");
        request.setEmail("new@example.com");

        UserBeneficiaries emptyBeneficiaries = new UserBeneficiaries();
        emptyBeneficiaries.setUserId(USERNAME);
        emptyBeneficiaries.setBeneficiaries(new ArrayList<>());

        when(beneficiariesRepository.findById(USERNAME)).thenReturn(Optional.of(emptyBeneficiaries));
        when(beneficiariesRepository.save(any(UserBeneficiaries.class))).thenReturn(emptyBeneficiaries);

        // Act
        ApiResponse<UserBeneficiaryResponse> response = userBeneficiariesService.addBeneficiary(authentication, request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Beneficiary added successfully", response.getMessage());
        assertEquals("New Beneficiary", response.getData().getName());
        assertEquals("new@example.com", response.getData().getEmail());
        verify(beneficiariesRepository).save(any(UserBeneficiaries.class));
    }

    @Test
    void addBeneficiary_DuplicateBeneficiary_ThrowsException() {
        // Arrange
        UserBeneficiaryRequest request = new UserBeneficiaryRequest();
        request.setName("Test Beneficiary");
        request.setEmail("test@example.com");

        when(beneficiariesRepository.findById(USERNAME)).thenReturn(Optional.of(userBeneficiaries));

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> 
            userBeneficiariesService.addBeneficiary(authentication, request)
        );
        verify(beneficiariesRepository, never()).save(any(UserBeneficiaries.class));
    }

    @Test
    void deleteBeneficiary_Success() {
        // Arrange
        when(beneficiariesRepository.findById(USERNAME)).thenReturn(Optional.of(userBeneficiaries));
        when(beneficiariesRepository.save(any(UserBeneficiaries.class))).thenReturn(userBeneficiaries);

        // Act
        ApiResponse<String> response = userBeneficiariesService.deleteBeneficiary(authentication, BENEFICIARY_ID);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Beneficiary deleted successfully", response.getMessage());
        verify(beneficiariesRepository).save(any(UserBeneficiaries.class));
    }

    @Test
    void deleteBeneficiary_BeneficiaryNotFound_ThrowsException() {
        // Arrange
        when(beneficiariesRepository.findById(USERNAME)).thenReturn(Optional.of(userBeneficiaries));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            userBeneficiariesService.deleteBeneficiary(authentication, 999L)
        );
        verify(beneficiariesRepository, never()).save(any(UserBeneficiaries.class));
    }

    @Test
    void getBeneficiary_Success() {
        // Arrange
        when(beneficiariesRepository.findById(USERNAME)).thenReturn(Optional.of(userBeneficiaries));

        // Act
        ApiResponse<UserBeneficiaryResponse> response = userBeneficiariesService.getBeneficiary(authentication, BENEFICIARY_ID);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Beneficiary retrieved successfully", response.getMessage());
        assertEquals(BENEFICIARY_ID, response.getData().getId());
        assertEquals("Test Beneficiary", response.getData().getName());
        assertEquals("test@example.com", response.getData().getEmail());
    }

    @Test
    void getBeneficiary_BeneficiaryNotFound_ThrowsException() {
        // Arrange
        when(beneficiariesRepository.findById(USERNAME)).thenReturn(Optional.of(userBeneficiaries));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            userBeneficiariesService.getBeneficiary(authentication, 999L)
        );
    }

    @Test
    void getBeneficiaries_Success() {
        // Arrange
        when(beneficiariesRepository.findById(USERNAME)).thenReturn(Optional.of(userBeneficiaries));

        // Act
        ApiResponse<UserBeneficiariesResponse> response = userBeneficiariesService.getBeneficiaries(authentication);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Beneficiaries retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().getBeneficiaries().size());
        assertEquals(BENEFICIARY_ID, response.getData().getBeneficiaries().get(0).getId());
    }

    @Test
    void getBeneficiaries_NoBeneficiaries_CreatesNewUserBeneficiaries() {
        // Arrange
        when(beneficiariesRepository.findById(USERNAME)).thenReturn(Optional.empty());
        
        UserBeneficiaries newUserBeneficiaries = new UserBeneficiaries();
        newUserBeneficiaries.setUserId(USERNAME);
        newUserBeneficiaries.setBeneficiaries(new ArrayList<>());
        
        // Act
        ApiResponse<UserBeneficiariesResponse> response = userBeneficiariesService.getBeneficiaries(authentication);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Beneficiaries retrieved successfully", response.getMessage());
        assertTrue(response.getData().getBeneficiaries().isEmpty());
    }

    @Test
    void getUserBeneficiariesOrThrow_NotFound_ThrowsException() {
        // Arrange
        when(beneficiariesRepository.findById(USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            userBeneficiariesService.getBeneficiary(authentication, BENEFICIARY_ID)
        );
    }
}