package com.shizzy.moneytransfer.s3;

import com.shizzy.moneytransfer.exception.InvalidFileFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    @Value("${aws.s3.buckets.wavesend}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.url-expiration:3600}") // Default 1 hour expiration if not specified
    private long urlExpirationSeconds;

    private final S3Client s3Client;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private final String [] acceptedFileExtensions = {".pdf", ".doc", ".png", ".jpg", ".jpeg"};
    private final String [] acceptedProfileImageExtensions = {".jpg", ".jpeg", ".png"};


    public void validateProfileImage(MultipartFile file) throws InvalidFileFormatException {
        String fileName = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
        boolean isValidExtension = false;
        for (String extension : acceptedProfileImageExtensions) {
            if (fileName.endsWith(extension)) {
                isValidExtension = true;
                break;
            }
        }

        if (!isValidExtension) {
            throw new InvalidFileFormatException("Profile image must be a JPG, JPEG, or PNG");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must not exceed 10 MB");
        }
    }


    /**
     * Generate a pre-signed URL for the S3 object with specified expiration
     *
     * @param key The object key in S3
     * @return A pre-signed URL that can be used to access the file
     */
    public String generatePresignedUrl(String key) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build()) {
                
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(urlExpirationSeconds))
                    .getObjectRequest(b -> b.bucket(bucketName).key(key))
                    .build();
    
            // Get the presigned URL and return it directly
            String presignedUrl = presigner.presignGetObject(presignRequest).url().toString();
            log.debug("Generated presigned URL for key {}: {}", key, presignedUrl);
            
            return presignedUrl;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", key, e);
            // Fallback to direct URL if presigning fails
            return generateDirectUrl(key);
        }
    }

    /**
     * Generate a direct S3 URL without presigning (public access required)
     */
    private String generateDirectUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    /**
     * Generate file URL based on configuration
     */
    public String generateFileUrl(String key) {
        return generatePresignedUrl(key);
    }

    public void validateKycDocument(MultipartFile file) throws InvalidFileFormatException {
        String fileName = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
        boolean isValidExtension = false;
        for (String extension : acceptedFileExtensions) {
            if (fileName.endsWith(extension)) {
                isValidExtension = true;
                break;
            }
        }

        if (!isValidExtension) {
            throw new InvalidFileFormatException("KYC document must be a PDF, DOC, PNG, JPG, or JPEG");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileFormatException("File size must not exceed 10 MB");
        }
    }

    public String uploadFileToS3(@NotNull MultipartFile file, String key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            return generateFileUrl(key);
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload file to S3 "+ e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }

    public byte[] getFileFromS3(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            try (InputStream inputStream = s3Client.getObject(getObjectRequest);
                 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
                return byteArrayOutputStream.toByteArray();
            }
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new RuntimeException("File not found in S3 with key: " + key);
            } else {
                throw new RuntimeException("Failed to retrieve file from S3", e);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading file from S3", e);
        }
    }

//    private String generateFileUrl(String key) {
//        S3Presigner presigner = S3Presigner.builder().region(Region.of(region)).build();
//        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
//                .signatureDuration(java.time.Duration.ofMinutes(60))
//                .getObjectRequest(b -> b.bucket(bucketName).key(key))
//                .build();
//
//        String presignedUrl = presigner.presignGetObject(presignRequest).url().toString();
//        presigner.close();
//
//        return presignedUrl;
//    }

    // private String generateFileUrl(String key) {
    //     return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    // }
}