package com.shizzy.moneytransfer.serviceimpl;



import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.InvalidFileFormatException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.UserProfileImage;
import com.shizzy.moneytransfer.repository.UserProfileImageRepository;
import com.shizzy.moneytransfer.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
class UserProfileImageServiceImplTest {

    @Mock
    private UserProfileImageRepository userProfileImageRepository;
    
    @Mock
    private S3Service s3Service;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private MultipartFile multipartFile;
    
    @InjectMocks
    private UserProfileImageServiceImpl userProfileImageService;
    
    private final String userId = "test-user-id";
    private final String imageUrl = "https://wavesend.s3.eu-west-1.amazonaws.com/profile-images/test-user-id/profile.jpg";
    private final String profileImageKey = "profile-images/test-user-id/profile.jpg";
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userProfileImageService, "bucketName", "wavesend");
        when(authentication.getName()).thenReturn(userId);
    }
    
    @Test
    void updateUserProfileImage_WhenProfileImageExists_ThenUpdateIt() {
        // Arrange
        UserProfileImage existingImage = new UserProfileImage();
        existingImage.setImageUrl("old-url");
        existingImage.setCreatedBy(userId);
        
        when(userProfileImageRepository.findByCreatedBy(userId)).thenReturn(Optional.of(existingImage));
        
        // Act
        userProfileImageService.updateUserProfileImage(imageUrl, authentication);
        
        // Assert
        verify(userProfileImageRepository).updateUserProfileImage(imageUrl, userId);
        verify(userProfileImageRepository, never()).save(any(UserProfileImage.class));
    }
    
    @Test
    void updateUserProfileImage_WhenProfileImageDoesNotExist_ThenCreateNewOne() {
        // Arrange
        when(userProfileImageRepository.findByCreatedBy(userId)).thenReturn(Optional.empty());
        
        // Act
        userProfileImageService.updateUserProfileImage(imageUrl, authentication);
        
        // Assert
        verify(userProfileImageRepository, never()).updateUserProfileImage(anyString(), anyString());
        verify(userProfileImageRepository).save(any(UserProfileImage.class));
    }
    
    @Test
    void getUserProfileImage_WhenProfileImageExists_ThenReturnBytes() {
        // Arrange
        UserProfileImage userProfileImage = new UserProfileImage();
        userProfileImage.setImageUrl(imageUrl);
        userProfileImage.setCreatedBy(userId);
        
        byte[] expectedBytes = "test-image-data".getBytes();
        
        when(userProfileImageRepository.findByCreatedBy(userId)).thenReturn(Optional.of(userProfileImage));
        when(s3Service.getFileFromS3(anyString())).thenReturn(expectedBytes);
        
        // Act
        byte[] result = userProfileImageService.getUserProfileImage(authentication);
        
        // Assert
        assertArrayEquals(expectedBytes, result);
        verify(s3Service).getFileFromS3(anyString());
    }
    
    @Test
    void getUserProfileImage_WhenProfileImageDoesNotExist_ThenThrowException() {
        // Arrange
        when(userProfileImageRepository.findByCreatedBy(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userProfileImageService.getUserProfileImage(authentication));
    }
    
    @Test
    void uploadUserProfileImage_WhenValidImage_ThenUploadSuccessfully() throws InvalidFileFormatException {
        // Arrange
        String filename = "profile.jpg";
        String presignedUrl = "https://presigned-url.com";
        
        when(multipartFile.getOriginalFilename()).thenReturn(filename);
        when(s3Service.uploadFileToS3(eq(multipartFile), anyString())).thenReturn(imageUrl);
        when(s3Service.generatePresignedUrl(anyString())).thenReturn(presignedUrl);
        
        // Act
        ApiResponse<String> response = userProfileImageService.uploadUserProfileImage(authentication, multipartFile);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals("User profile image uploaded successfully", response.getMessage());
        assertEquals(presignedUrl, response.getData());
        
        verify(s3Service).validateProfileImage(multipartFile);
        verify(s3Service).uploadFileToS3(eq(multipartFile), anyString());
        verify(s3Service).generatePresignedUrl(anyString());
    }
    
    @Test
    void uploadUserProfileImage_WhenValidationFails_ThenThrowException() throws InvalidFileFormatException {
        // Arrange
        doThrow(new InvalidFileFormatException("Invalid file format")).when(s3Service).validateProfileImage(multipartFile);
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> userProfileImageService.uploadUserProfileImage(authentication, multipartFile));
    }
    
    @Test
    void getUserProfileImageUrl_WhenProfileImageExists_ThenReturnPresignedUrl() {
        // Arrange
        UserProfileImage userProfileImage = new UserProfileImage();
        userProfileImage.setImageUrl(imageUrl);
        userProfileImage.setCreatedBy(userId);
        
        String presignedUrl = "https://presigned-url.com";
        
        when(userProfileImageRepository.findByCreatedBy(userId)).thenReturn(Optional.of(userProfileImage));
        when(s3Service.generatePresignedUrl(anyString())).thenReturn(presignedUrl);
        
        // Act
        ApiResponse<String> response = userProfileImageService.getUserProfileImageUrl(authentication);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals("User profile image URL retrieved successfully", response.getMessage());
        assertEquals(presignedUrl, response.getData());
    }
    
    @Test
    void getUserProfileImageUrl_WhenProfileImageDoesNotExist_ThenThrowException() {
        // Arrange
        when(userProfileImageRepository.findByCreatedBy(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userProfileImageService.getUserProfileImageUrl(authentication));
    }
    
    @Test
    void uploadUserProfileImage_WithDangerousFilename_ThenSanitizeIt() throws InvalidFileFormatException {
        // Arrange
        String dangerousFilename = "../../../dangerous/file.jpg";
        
        when(multipartFile.getOriginalFilename()).thenReturn(dangerousFilename);
        when(s3Service.uploadFileToS3(eq(multipartFile), anyString())).thenReturn(imageUrl);
        when(s3Service.generatePresignedUrl(anyString())).thenReturn("presigned-url");
        
        // Act
        ApiResponse<String> response = userProfileImageService.uploadUserProfileImage(authentication, multipartFile);
        
        // Assert
        assertTrue(response.isSuccess());
        verify(s3Service).uploadFileToS3(eq(multipartFile), argThat(path -> !path.contains("../")));
    }
    
    @Test
    void uploadUserProfileImage_WithNullFilename_ThenUseDefaultName() throws InvalidFileFormatException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn(null);
        when(s3Service.uploadFileToS3(eq(multipartFile), contains("profile-"))).thenReturn(imageUrl);
        when(s3Service.generatePresignedUrl(anyString())).thenReturn("presigned-url");
        
        // Act
        ApiResponse<String> response = userProfileImageService.uploadUserProfileImage(authentication, multipartFile);
        
        // Assert
        assertTrue(response.isSuccess());
        verify(s3Service).uploadFileToS3(eq(multipartFile), argThat(path -> path.contains("profile-") && path.contains(".jpg")));
    }
    
    @Test
    void getUserProfileImageUrl_WithFullUrl_ThenExtractKeyCorrectly() {
        // Arrange
        UserProfileImage userProfileImage = new UserProfileImage();
        userProfileImage.setImageUrl("https://wavesend.s3.eu-west-1.amazonaws.com/profile-images/test-user-id/image.jpg");
        userProfileImage.setCreatedBy(userId);
        
        when(userProfileImageRepository.findByCreatedBy(userId)).thenReturn(Optional.of(userProfileImage));
        when(s3Service.generatePresignedUrl(anyString())).thenReturn("presigned-url");
        
        // Act
        userProfileImageService.getUserProfileImageUrl(authentication);
        
        // Assert
        verify(s3Service).generatePresignedUrl(anyString());
    }
    
    @Test
    void getUserProfileImageUrl_WithRelativePath_ThenUsePathAsKey() {
        // Arrange
        String relativePath = "profile-images/test-user-id/image.jpg";
        UserProfileImage userProfileImage = new UserProfileImage();
        userProfileImage.setImageUrl(relativePath);
        userProfileImage.setCreatedBy(userId);
        
        when(userProfileImageRepository.findByCreatedBy(userId)).thenReturn(Optional.of(userProfileImage));
        when(s3Service.generatePresignedUrl(relativePath)).thenReturn("presigned-url");
        
        // Act
        userProfileImageService.getUserProfileImageUrl(authentication);
        
        // Assert
        verify(s3Service).generatePresignedUrl(relativePath);
    }
}