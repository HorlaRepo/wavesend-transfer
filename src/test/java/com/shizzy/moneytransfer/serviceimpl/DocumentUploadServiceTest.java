package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private MultipartFile document;

    @InjectMocks
    private DocumentUploadService documentUploadService;

    private final String userId = "user123";
    private final String documentType = "passport";
    private final String fileName = "passport.jpg";
    private final String expectedS3Url = "https://s3-bucket.com/file-path";
    private final String expectedKey = String.format("kyc-documents/%s/%s-docs/%s", userId, documentType, fileName);

    @BeforeEach
    void setUp() {
        lenient().when(document.getOriginalFilename()).thenReturn(fileName);
    }

    @Test
    void uploadDocument_shouldCallValidateAndUploadToS3() {
        // Arrange
        when(s3Service.uploadFileToS3(document, expectedKey)).thenReturn(expectedS3Url);
        
        // Act
        String result = documentUploadService.uploadDocument(document, userId, documentType);
        
        // Assert
        assertEquals(expectedS3Url, result);
        verify(s3Service).validateKycDocument(document);
        verify(s3Service).uploadFileToS3(document, expectedKey);
    }

    @Test
    void uploadDocument_whenValidationFails_shouldPropagateException() {
        // Arrange
        doThrow(new RuntimeException("Invalid document")).when(s3Service).validateKycDocument(document);
        
        // Act & Assert
        try {
            documentUploadService.uploadDocument(document, userId, documentType);
        } catch (RuntimeException e) {
            verify(s3Service, times(1)).validateKycDocument(document);
            verify(s3Service, never()).uploadFileToS3(any(), any());
        }
    }
}