package com.shizzy.moneytransfer.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class KeyVaultSecretProvider {
    private static final Logger log = LoggerFactory.getLogger(KeyVaultSecretProvider.class);

    @Value("${azure.keyvault.url:https://wavesend.vault.azure.net/}")
    private String keyVaultUrl;

    private final SecretClient secretClient;
    private final Map<String, String> secretCache = new HashMap<>();

    public KeyVaultSecretProvider() {
        this.secretClient = new SecretClientBuilder()
                .vaultUrl("https://wavesend.vault.azure.net/")
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    @PostConstruct
    public void init() {
        try {
            // Pre-load common secrets
            loadSecret("aws-access-key-id");
            loadSecret("aws-secret-access-key");
            log.info("Successfully loaded secrets from Azure Key Vault");
        } catch (Exception e) {
            log.error("Error loading secrets from Azure Key Vault: {}", e.getMessage(), e);
        }
    }

    private void loadSecret(String secretName) {
        try {
            KeyVaultSecret secret = secretClient.getSecret(secretName);
            secretCache.put(secretName, secret.getValue());
        } catch (Exception e) {
            log.warn("Could not load secret '{}': {}", secretName, e.getMessage());
        }
    }

    public String getSecret(String secretName) {
        if (secretCache.containsKey(secretName)) {
            return secretCache.get(secretName);
        }
        
        try {
            KeyVaultSecret secret = secretClient.getSecret(secretName);
            String value = secret.getValue();
            secretCache.put(secretName, value);
            return value;
        } catch (Exception e) {
            log.error("Error retrieving secret '{}': {}", secretName, e.getMessage());
            return null;
        }
    }
}