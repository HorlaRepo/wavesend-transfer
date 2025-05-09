package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.enums.VerificationLevel;
import com.shizzy.moneytransfer.enums.VerificationStatus;
import com.shizzy.moneytransfer.exception.InvalidFileFormatException;
import com.shizzy.moneytransfer.exception.InvalidRequestException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.KycVerification;
import com.shizzy.moneytransfer.model.UserAccountLimit;
import com.shizzy.moneytransfer.repository.KycVerificationRepository;
import com.shizzy.moneytransfer.repository.UserAccountLimitRepository;
import com.shizzy.moneytransfer.s3.S3Service;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;





public class KycVerificationServiceImplTest {

    private KycVerificationServiceImpl kycVerificationService;

    @Mock
    private KycVerificationRepository kycVerificationRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private DocumentUploadService documentUploadService;

    @Mock
    private VerificationStatusService verificationStatusService;

    @Mock
    private UserAccountLimitRepository userAccountLimitRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private MultipartFile multipartFile;

    private static final String USER_ID = "test-user-id";
    private static final String DOCUMENT_URL = "https://example.com/document.pdf";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        kycVerificationService = new KycVerificationServiceImpl(
                kycVerificationRepository,
                s3Service,
                documentUploadService,
                verificationStatusService,
                userAccountLimitRepository
        );

        when(authentication.getName()).thenReturn(USER_ID);
    }

    @Test
    public void uploadIdDocument_Success() throws InvalidFileFormatException {
        // Arrange
        when(documentUploadService.uploadDocument(any(MultipartFile.class), anyString(), eq("id")))
                .thenReturn(DOCUMENT_URL);
        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act
        ApiResponse<String> response = kycVerificationService.uploadIdDocument(authentication, multipartFile);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("ID document uploaded successfully", response.getMessage());
        verify(kycVerificationRepository).save(any(KycVerification.class));
    }

    @Test
    public void uploadAddressDocument_Success() throws InvalidFileFormatException {
        // Arrange
        KycVerification existingKyc = new KycVerification();
        existingKyc.setId(1L);
        existingKyc.setUserId(USER_ID);

        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingKyc));
        when(documentUploadService.uploadDocument(any(MultipartFile.class), anyString(), eq("address")))
                .thenReturn(DOCUMENT_URL);

        // Act
        ApiResponse<String> response = kycVerificationService.uploadAddressDocument(authentication, multipartFile);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Address document uploaded successfully", response.getMessage());
        verify(kycVerificationRepository).save(any(KycVerification.class));
    }

    @Test(expected = InvalidRequestException.class)
    public void uploadAddressDocument_ThrowsExceptionWhenIdVerificationNotCompleted() {
        // Arrange
        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new KycVerification()));

        // Act - should throw exception
        kycVerificationService.uploadAddressDocument(authentication, multipartFile);
    }

    @Test
    public void approveIdVerification_Success() {
        // Arrange
        when(userAccountLimitRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act
        ApiResponse<String> response = kycVerificationService.approveIdVerification(USER_ID);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("ID verification approved successfully", response.getMessage());
        verify(verificationStatusService).updateVerificationStatus(USER_ID, "id", VerificationStatus.APPROVED, null);
        verify(userAccountLimitRepository).save(any(UserAccountLimit.class));
    }

    @Test
    public void approveAddressVerification_Success_WithIdAlreadyApproved() {
        // Arrange
        KycVerification kyc = new KycVerification();
        kyc.setIdVerificationStatus(VerificationStatus.APPROVED);
        
        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(kyc));
        when(userAccountLimitRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act
        ApiResponse<String> response = kycVerificationService.approveAddressVerification(USER_ID);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Address verification approved successfully", response.getMessage());
        verify(verificationStatusService).updateVerificationStatus(USER_ID, "address", VerificationStatus.APPROVED, null);
        
        // Verify user was set to fully verified level
        verify(userAccountLimitRepository).save(argThat(limit -> 
            limit.getVerificationLevel() == VerificationLevel.FULLY_VERIFIED
        ));
    }

    @Test
    public void rejectIdVerification_Success() {
        // Arrange
        String rejectionReason = "Document is unclear";

        // Act
        ApiResponse<String> response = kycVerificationService.rejectIdVerification(USER_ID, rejectionReason);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("ID verification rejected successfully", response.getMessage());
        verify(verificationStatusService).updateVerificationStatus(USER_ID, "address", VerificationStatus.REJECTED, rejectionReason);
    }

    @Test
    public void rejectAddressVerification_Success() {
        // Arrange
        String rejectionReason = "Address document expired";

        // Act
        ApiResponse<String> response = kycVerificationService.rejectAddressVerification(USER_ID, rejectionReason);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Address verification rejected successfully", response.getMessage());
        verify(verificationStatusService).updateVerificationStatus(USER_ID, "id", VerificationStatus.REJECTED, rejectionReason);
    }

    @Test
    public void updateKyc_Success() {
        // Arrange
        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act
        ApiResponse<String> response = kycVerificationService.updateKyc(
                authentication, "new-address-url", "new-id-url");

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("KYC documents updated successfully", response.getMessage());
        verify(kycVerificationRepository).save(argThat(kyc -> 
            kyc.getIdDocumentUrl().equals("new-id-url") && 
            kyc.getAddressDocumentUrl().equals("new-address-url") &&
            kyc.getIdVerificationStatus() == VerificationStatus.PENDING &&
            kyc.getAddressVerificationStatus() == VerificationStatus.PENDING
        ));
    }

    @Test
    public void getKycStatus_Success() {
        // Arrange
        KycVerification kyc = new KycVerification();
        kyc.setUserId(USER_ID);
        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(kyc));

        // Act
        ApiResponse<KycVerification> response = kycVerificationService.getKycStatus(authentication);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("KYC verification data found", response.getMessage());
        assertSame(kyc, response.getData());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void getKycStatus_ThrowsExceptionWhenNotFound() {
        // Arrange
        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act - should throw exception
        kycVerificationService.getKycStatus(authentication);
    }
}