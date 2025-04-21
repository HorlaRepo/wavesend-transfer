package com.shizzy.moneytransfer.service;

import java.util.Map;
import java.util.Set;

public interface CacheService {
    /**
     * Get a cached value by key from a specific cache
     * @param cacheName Name of the cache
     * @param key Key to retrieve
     * @return The cached value or null if not found
     */
    Object get(String cacheName, String key);
    
    /**
     * Store a value in the cache
     * @param cacheName Name of the cache
     * @param key Key to store under
     * @param value Value to cache
     * @param ttlInSeconds Time to live in seconds
     */
    void put(String cacheName, String key, Object value, long ttlInSeconds);
    
    /**
     * Remove a specific key from the cache
     * @param cacheName Name of the cache
     * @param key Key to remove
     * @return true if removed, false if not found
     */
    boolean evict(String cacheName, String key);
    
    /**
     * Clear an entire cache
     * @param cacheName Name of the cache to clear
     */
    void clear(String cacheName);
    
    /**
     * Get all keys in a specific cache
     * @param cacheName Name of the cache
     * @return Set of keys
     */
    Set<String> getKeys(String cacheName);
    
    /**
     * Get cache statistics
     * @return Map of cache statistics
     */
    Map<String, Map<String, Object>> getCacheStats();
}