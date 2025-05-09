package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.enums.VerificationStatus;
import com.shizzy.moneytransfer.model.KycVerification;
import com.shizzy.moneytransfer.repository.KycVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static com.shizzy.moneytransfer.enums.VerificationStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
class VerificationStatusServiceTest {

    @Mock
    private KycVerificationRepository kycVerificationRepository;

    @InjectMocks
    private VerificationStatusService verificationStatusService;

    @Captor
    private ArgumentCaptor<KycVerification> kycCaptor;

    private static final String USER_ID = "user123";
    private static final String REJECTION_REASON = "Document unclear";

    @Test
    void updateIdVerificationStatus_WhenApproved_ShouldUpdateStatusWithoutRejectionReason() {
        // Arrange
        KycVerification existingKyc = new KycVerification();
        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingKyc));

        // Act
        verificationStatusService.updateVerificationStatus(USER_ID, "id", APPROVED, null);

        // Assert
        verify(kycVerificationRepository).save(kycCaptor.capture());
        KycVerification savedKyc = kycCaptor.getValue();
        assertEquals(USER_ID, savedKyc.getUserId());
        assertEquals(APPROVED, savedKyc.getIdVerificationStatus());
        assertNull(savedKyc.getIdRejectionReason());
    }

    @Test
    void updateIdVerificationStatus_WhenRejected_ShouldUpdateStatusWithRejectionReason() {
        // Arrange
        KycVerification existingKyc = new KycVerification();
        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingKyc));

        // Act
        verificationStatusService.updateVerificationStatus(USER_ID, "id", REJECTED, REJECTION_REASON);

        // Assert
        verify(kycVerificationRepository).save(kycCaptor.capture());
        KycVerification savedKyc = kycCaptor.getValue();
        assertEquals(USER_ID, savedKyc.getUserId());
        assertEquals(REJECTED, savedKyc.getIdVerificationStatus());
        assertEquals(REJECTION_REASON, savedKyc.getIdRejectionReason());
    }

    @Test
    void updateAddressVerificationStatus_WhenApproved_ShouldUpdateStatusWithoutRejectionReason() {
        // Arrange
        KycVerification existingKyc = new KycVerification();
        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingKyc));

        // Act
        verificationStatusService.updateVerificationStatus(USER_ID, "address", APPROVED, null);

        // Assert
        verify(kycVerificationRepository).save(kycCaptor.capture());
        KycVerification savedKyc = kycCaptor.getValue();
        assertEquals(USER_ID, savedKyc.getUserId());
        assertEquals(APPROVED, savedKyc.getAddressVerificationStatus());
        assertNull(savedKyc.getAddressRejectionReason());
    }

    @Test
    void updateAddressVerificationStatus_WhenRejected_ShouldUpdateStatusWithRejectionReason() {
        // Arrange
        KycVerification existingKyc = new KycVerification();
        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingKyc));

        // Act
        verificationStatusService.updateVerificationStatus(USER_ID, "address", REJECTED, REJECTION_REASON);

        // Assert
        verify(kycVerificationRepository).save(kycCaptor.capture());
        KycVerification savedKyc = kycCaptor.getValue();
        assertEquals(USER_ID, savedKyc.getUserId());
        assertEquals(REJECTED, savedKyc.getAddressVerificationStatus());
        assertEquals(REJECTION_REASON, savedKyc.getAddressRejectionReason());
    }

    @Test
    void updateVerificationStatus_WhenUserDoesNotExist_ShouldCreateNewRecord() {
        // Arrange
        when(kycVerificationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act
        verificationStatusService.updateVerificationStatus(USER_ID, "id", PENDING, null);

        // Assert
        verify(kycVerificationRepository).save(kycCaptor.capture());
        KycVerification savedKyc = kycCaptor.getValue();
        assertEquals(USER_ID, savedKyc.getUserId());
        assertEquals(PENDING, savedKyc.getIdVerificationStatus());
    }
}