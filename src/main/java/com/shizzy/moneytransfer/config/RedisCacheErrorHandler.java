package com.shizzy.moneytransfer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheErrorHandler implements CacheErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        logger.error("Cache GET operation failed for cache: {}, key: {}", cache.getName(), key, exception);
        // Continue execution without failing - allows graceful degradation when Redis is down
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        logger.error("Cache PUT operation failed for cache: {}, key: {}", cache.getName(), key, exception);
        // Continue execution without failing
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        logger.error("Cache EVICT operation failed for cache: {}, key: {}", cache.getName(), key, exception);
        // Continue execution without failing
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        logger.error("Cache CLEAR operation failed for cache: {}", cache.getName(), exception);
        // Continue execution without failing
    }
}