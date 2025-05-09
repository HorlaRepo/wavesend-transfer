package com.shizzy.moneytransfer.serviceimpl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.shizzy.moneytransfer.api.ApiResponse;
import com.shizzy.moneytransfer.exception.InvalidFileFormatException;
import com.shizzy.moneytransfer.exception.ResourceNotFoundException;
import com.shizzy.moneytransfer.model.UserProfileImage;
import com.shizzy.moneytransfer.repository.UserProfileImageRepository;
import com.shizzy.moneytransfer.s3.S3Service;
import com.shizzy.moneytransfer.service.UserProfileImageService;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class UserProfileImageServiceImpl implements UserProfileImageService {

    private final UserProfileImageRepository userProfileImageRepository;
    private final S3Service s3Service;

    @Value("${aws.s3.buckets.wavesend}")
    private String bucketName;

    @Override
    public void updateUserProfileImage(String profileImageUrl, Authentication connectedUser) {
        Optional<UserProfileImage> existingProfileImage = userProfileImageRepository.findByCreatedBy(connectedUser.getName());
        if (existingProfileImage.isPresent()) {
            userProfileImageRepository.updateUserProfileImage(profileImageUrl, connectedUser.getName());
        } else {
            UserProfileImage newUserProfileImage = new UserProfileImage();
            newUserProfileImage.setImageUrl(profileImageUrl);
            newUserProfileImage.setCreatedBy(connectedUser.getName());
            userProfileImageRepository.save(newUserProfileImage);
        }
    }

    @Override
    public byte[] getUserProfileImage(Authentication connectedUser) {

        String userId = connectedUser.getName();

        UserProfileImage userProfileImage = userProfileImageRepository.findByCreatedBy(connectedUser.getName()).orElseThrow(
                () -> new ResourceNotFoundException("User profile image not found"));

        String key = userProfileImage.getImageUrl().replace("https://wavesend.s3.eu-west-1.amazonaws.com/profile-images" + "/" + userId+"/", "");

        return s3Service.getFileFromS3("profile-images/"+userId+"/"+key);
    }

    @Override
    public ApiResponse<String> uploadUserProfileImage(Authentication connectedUser, MultipartFile userProfileImage)  {
        String userId = connectedUser.getName();
        try {
            s3Service.validateProfileImage(userProfileImage);
        } catch (InvalidFileFormatException e) {
            throw new RuntimeException(e);
        }
        String safeFilename = sanitizeFilename(userProfileImage.getOriginalFilename());
        String profileImageKey = "profile-images/" + userId + "/" + safeFilename;

        // Upload to S3 and get URL
        String profileImageUrl = s3Service.uploadFileToS3(userProfileImage, profileImageKey);

        String presignedUrl = s3Service.generatePresignedUrl(profileImageKey);


        updateUserProfileImage(profileImageUrl, connectedUser);

        return ApiResponse.<String>builder()
                .success(true)
                .message("User profile image uploaded successfully")
                .data(presignedUrl)
                .build();

    }


    @Override
    public ApiResponse<String> getUserProfileImageUrl(Authentication connectedUser) {
        String userId = connectedUser.getName();

        UserProfileImage userProfileImage = userProfileImageRepository.findByCreatedBy(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile image not found"));

        // Extract the S3 key from the stored URL
        String imageUrl = userProfileImage.getImageUrl();
        String key = extractS3KeyFromUrl(imageUrl, userId);
        
        // Generate a pre-signed URL for secure access
        String presignedUrl = s3Service.generatePresignedUrl(key);
        
        return ApiResponse.<String>builder()
                .success(true)
                .message("User profile image URL retrieved successfully")
                .data(presignedUrl)
                .build();
    }


    /**
     * Helper method to extract S3 key from URL
     */
    private String extractS3KeyFromUrl(String imageUrl, String userId) {
        // Handle both full URLs and relative paths
        if (imageUrl.startsWith("https://")) {
            // Remove the base S3 URL to get the key
            String baseUrl = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, "eu-west-1");
            return imageUrl.replace(baseUrl, "");
        } else {
            // Already a relative path
            return imageUrl;
        }
    }

    /**
     * Sanitize filename to prevent path traversal and other issues
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "profile-" + System.currentTimeMillis() + ".jpg";
        }
        
        // Remove path separators and dangerous characters
        String sanitized = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        // If the sanitization resulted in an empty string, use a default
        if (sanitized.trim().isEmpty()) {
            return "profile-" + System.currentTimeMillis() + ".jpg";
        }
        
        return sanitized;
    }

}
