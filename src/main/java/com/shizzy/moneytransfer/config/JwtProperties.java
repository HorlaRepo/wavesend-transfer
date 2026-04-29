package com.shizzy.moneytransfer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secretKey;
    private long accessTokenExpiration = 3600000; // 1 hour in milliseconds
    private long refreshTokenExpiration = 604800000; // 7 days in milliseconds
}
