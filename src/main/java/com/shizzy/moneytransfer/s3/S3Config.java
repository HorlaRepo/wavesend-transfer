package com.shizzy.moneytransfer.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class S3Config {
    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Value("${aws.region:eu-west-1}")
    private String awsRegion;

    @Value("${aws.access-key-id}")
    private String awsAccessKeyId;

    @Value("${aws.secret-access-key}")
    private String awsSecretAccessKey;

    

    @Bean
    public S3Client s3Client() {
        try {
                String envAccessKey = awsAccessKeyId;
                String envSecretKey = awsSecretAccessKey;

                if (envAccessKey != null && !envAccessKey.isEmpty() &&
                        envSecretKey != null && !envSecretKey.isEmpty()) {

                    log.info("Using AWS credentials from environment variables");
                    AwsBasicCredentials credentials = AwsBasicCredentials.create(
                            envAccessKey, envSecretKey);

                    return S3Client.builder()
                            .region(Region.of(awsRegion))
                            .credentialsProvider(StaticCredentialsProvider.create(credentials))
                            .build();
                }

                // Fall back to default credentials provider chain
                log.warn("No credentials found in Key Vault or environment, falling back to default credential chain");
                return S3Client.builder()
                        .region(Region.of(awsRegion))
                        .build();
            
        } catch (Exception e) {
            log.error("Error configuring S3 client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure S3 client: " + e.getMessage(), e);
        }
    }
    

    @Bean
    public S3Service s3Service(S3Client s3Client) {
        return new S3Service(s3Client);
    }
}