package com.shizzy.moneytransfer.s3;

import com.shizzy.moneytransfer.config.KeyVaultSecretProvider;
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

    private final KeyVaultSecretProvider secretProvider;

    public S3Config(KeyVaultSecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    @Bean
    public S3Client s3Client() {
        try {
            String awsAccessKeyId = secretProvider.getSecret("aws-access-key-id");
            String awsSecretAccessKey = secretProvider.getSecret("aws-secret-access-key");

            log.info("Attempting to configure S3 client for region: {}", awsRegion);

            if (awsAccessKeyId != null && !awsAccessKeyId.isEmpty() &&
                    awsSecretAccessKey != null && !awsSecretAccessKey.isEmpty()) {

                log.info("Successfully retrieved AWS credentials from Key Vault");

                // Create credentials provider
                AwsBasicCredentials credentials = AwsBasicCredentials.create(
                        awsAccessKeyId,
                        awsSecretAccessKey);

                // Return client with credentials
                return S3Client.builder()
                        .region(Region.of(awsRegion))
                        .credentialsProvider(StaticCredentialsProvider.create(credentials))
                        .build();
            } else {
                log.warn("Key Vault secrets not found or empty, checking environment variables");

                // Check environment variables directly as a last resort
                String envAccessKey = System.getenv("AWS_ACCESS_KEY_ID");
                String envSecretKey = System.getenv("AWS_SECRET_ACCESS_KEY");

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
            }
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