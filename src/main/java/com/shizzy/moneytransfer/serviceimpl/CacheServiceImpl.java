package com.shizzy.moneytransfer.serviceimpl;

import com.shizzy.moneytransfer.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheServiceImpl.class);
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Object get(String cacheName, String key) {
        String fullKey = cacheName + "::" + key;
        Object value = redisTemplate.opsForValue().get(fullKey);
        logger.debug("Cache get - key: {}, found: {}", fullKey, value != null);
        return value;
    }

    @Override
    public void put(String cacheName, String key, Object value, long ttlInSeconds) {
        String fullKey = cacheName + "::" + key;
        redisTemplate.opsForValue().set(fullKey, value, ttlInSeconds, TimeUnit.SECONDS);
        logger.debug("Cache put - key: {}", fullKey);
    }

    @Override
    public boolean evict(String cacheName, String key) {
        String fullKey = cacheName + "::" + key;
        Boolean result = redisTemplate.delete(fullKey);
        logger.debug("Cache evict - key: {}, success: {}", fullKey, result);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void clear(String cacheName) {
        Set<String> keys = redisTemplate.keys(cacheName + "::*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.debug("Cache clear - cache: {}, keys removed: {}", cacheName, keys.size());
        }
    }

    @Override
    public Set<String> getKeys(String cacheName) {
        return redisTemplate.keys(cacheName + "::*");
    }

    @Override
    public Map<String, Map<String, Object>> getCacheStats() {
        Map<String, Map<String, Object>> stats = new HashMap<>();
        
        // Get all cache names by pattern scan
        Set<String> cachePatterns = redisTemplate.keys("*::*");
        if (cachePatterns == null || cachePatterns.isEmpty()) {
            return stats;
        }
        
        // Extract unique cache names
        Set<String> cacheNames = cachePatterns.stream()
                .map(key -> key.split("::")[0])
                .collect(java.util.stream.Collectors.toSet());
                
        // Collect stats for each cache
        for (String cacheName : cacheNames) {
            Map<String, Object> cacheStats = new HashMap<>();
            Set<String> keys = redisTemplate.keys(cacheName + "::*");
            
            if (keys != null) {
                cacheStats.put("size", keys.size());
                cacheStats.put("keys", keys);
            }
            
            stats.put(cacheName, cacheStats);
        }
        
        return stats;
    }
}