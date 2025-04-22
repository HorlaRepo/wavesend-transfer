package com.shizzy.moneytransfer.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableCaching
@Profile("prod")
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false")
public class RedisFallbackConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisFallbackConfig.class);

    @Bean
    @Primary
    public CacheManager noOpCacheManager() {
        logger.info("Redis disabled, using NoOp cache manager");
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        Cache transactions = new NoOpCache("transactions");
        Cache wallets = new NoOpCache("wallets");
        Cache otp = new NoOpCache("otp");
        cacheManager.setCaches(Arrays.asList(transactions, wallets, otp));
        return cacheManager;
    }

    
}
