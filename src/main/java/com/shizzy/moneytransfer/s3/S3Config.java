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
        String awsAccessKeyId = secretProvider.getSecret("aws-access-key-id");
        String awsSecretAccessKey = secretProvider.getSecret("aws-secret-access-key");
        
        if (awsAccessKeyId != null && !awsAccessKeyId.isEmpty() && 
            awsSecretAccessKey != null && !awsSecretAccessKey.isEmpty()) {
            
            log.info("Configuring S3 client with Key Vault credentials for region: {}", awsRegion);
            
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
            // Fall back to default credentials provider chain
            log.warn("Key Vault secrets not found, falling back to default credential chain");
            return S3Client.builder()
                    .region(Region.of(awsRegion))
                    .build();
        }
    }

    @Bean
    public S3Service s3Service(S3Client s3Client) {
        return new S3Service(s3Client);
    }
}