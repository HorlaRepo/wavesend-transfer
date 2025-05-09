package com.shizzy.moneytransfer.serviceimpl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;





@ExtendWith(MockitoExtension.class)
class CacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    private CacheServiceImpl cacheService;
    
    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new CacheServiceImpl(redisTemplate);
    }
    
    @Test
    void get_ShouldReturnCachedValue() {
        // Arrange
        String cacheName = "testCache";
        String key = "testKey";
        String fullKey = cacheName + "::" + key;
        Object expectedValue = "testValue";
        
        when(valueOperations.get(fullKey)).thenReturn(expectedValue);
        
        // Act
        Object result = cacheService.get(cacheName, key);
        
        // Assert
        assertEquals(expectedValue, result);
        verify(valueOperations).get(fullKey);
    }
    
    @Test
    void put_ShouldStoreValueInCache() {
        // Arrange
        String cacheName = "testCache";
        String key = "testKey";
        String fullKey = cacheName + "::" + key;
        Object value = "testValue";
        long ttl = 60;
        
        // Act
        cacheService.put(cacheName, key, value, ttl);
        
        // Assert
        verify(valueOperations).set(fullKey, value, ttl, TimeUnit.SECONDS);
    }
    
    @Test
    void evict_ShouldRemoveKeyFromCache_AndReturnTrue_WhenKeyExists() {
        // Arrange
        String cacheName = "testCache";
        String key = "testKey";
        String fullKey = cacheName + "::" + key;
        
        when(redisTemplate.delete(fullKey)).thenReturn(true);
        
        // Act
        boolean result = cacheService.evict(cacheName, key);
        
        // Assert
        assertTrue(result);
        verify(redisTemplate).delete(fullKey);
    }
    
    @Test
    void evict_ShouldReturnFalse_WhenKeyDoesNotExist() {
        // Arrange
        String cacheName = "testCache";
        String key = "testKey";
        String fullKey = cacheName + "::" + key;
        
        when(redisTemplate.delete(fullKey)).thenReturn(false);
        
        // Act
        boolean result = cacheService.evict(cacheName, key);
        
        // Assert
        assertFalse(result);
        verify(redisTemplate).delete(fullKey);
    }
    
    @Test
    void clear_ShouldRemoveAllKeysForCache() {
        // Arrange
        String cacheName = "testCache";
        Set<String> keys = new HashSet<>();
        keys.add(cacheName + "::key1");
        keys.add(cacheName + "::key2");
        
        when(redisTemplate.keys(cacheName + "::*")).thenReturn(keys);
        
        // Act
        cacheService.clear(cacheName);
        
        // Assert
        verify(redisTemplate).keys(cacheName + "::*");
        verify(redisTemplate).delete(keys);
    }
    
    @Test
    void clear_ShouldDoNothing_WhenNoKeysExist() {
        // Arrange
        String cacheName = "testCache";
        
        when(redisTemplate.keys(cacheName + "::*")).thenReturn(null);
        
        // Act
        cacheService.clear(cacheName);
        
        // Assert
        verify(redisTemplate).keys(cacheName + "::*");
        verify(redisTemplate, never()).delete(anySet());
    }
    
    @Test
    void getKeys_ShouldReturnKeysForCache() {
        // Arrange
        String cacheName = "testCache";
        Set<String> expectedKeys = new HashSet<>();
        expectedKeys.add(cacheName + "::key1");
        expectedKeys.add(cacheName + "::key2");
        
        when(redisTemplate.keys(cacheName + "::*")).thenReturn(expectedKeys);
        
        // Act
        Set<String> result = cacheService.getKeys(cacheName);
        
        // Assert
        assertEquals(expectedKeys, result);
        verify(redisTemplate).keys(cacheName + "::*");
    }
    
    @Test
    void getCacheStats_ShouldReturnStats_WhenCachesExist() {
        // Arrange
        Set<String> allKeys = new HashSet<>();
        allKeys.add("cache1::key1");
        allKeys.add("cache1::key2");
        allKeys.add("cache2::key1");
        
        Set<String> cache1Keys = new HashSet<>();
        cache1Keys.add("cache1::key1");
        cache1Keys.add("cache1::key2");
        
        Set<String> cache2Keys = new HashSet<>();
        cache2Keys.add("cache2::key1");
        
        when(redisTemplate.keys("*::*")).thenReturn(allKeys);
        when(redisTemplate.keys("cache1::*")).thenReturn(cache1Keys);
        when(redisTemplate.keys("cache2::*")).thenReturn(cache2Keys);
        
        // Act
        Map<String, Map<String, Object>> result = cacheService.getCacheStats();
        
        // Assert
        assertEquals(2, result.size());
        assertTrue(result.containsKey("cache1"));
        assertTrue(result.containsKey("cache2"));
        
        Map<String, Object> cache1Stats = result.get("cache1");
        assertEquals(2, cache1Stats.get("size"));
        assertEquals(cache1Keys, cache1Stats.get("keys"));
        
        Map<String, Object> cache2Stats = result.get("cache2");
        assertEquals(1, cache2Stats.get("size"));
        assertEquals(cache2Keys, cache2Stats.get("keys"));
        
        verify(redisTemplate).keys("*::*");
        verify(redisTemplate).keys("cache1::*");
        verify(redisTemplate).keys("cache2::*");
    }
    
    @Test
    void getCacheStats_ShouldReturnEmptyMap_WhenNoCachesExist() {
        // Arrange
        when(redisTemplate.keys("*::*")).thenReturn(null);
        
        // Act
        Map<String, Map<String, Object>> result = cacheService.getCacheStats();
        
        // Assert
        assertTrue(result.isEmpty());
        verify(redisTemplate).keys("*::*");
    }
}