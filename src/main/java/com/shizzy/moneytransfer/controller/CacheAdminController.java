package com.shizzy.moneytransfer.controller;

import com.shizzy.moneytransfer.service.CacheService;
import com.shizzy.moneytransfer.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CacheAdminController {

    private final CacheService cacheService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Map<String, Object>>> getCacheStats() {
        return ApiResponse.<Map<String, Map<String, Object>>>builder()
                .success(true)
                .message("Cache statistics retrieved successfully")
                .data(cacheService.getCacheStats())
                .build();
    }

    @GetMapping("/{cacheName}/keys")
    public ApiResponse<Set<String>> getCacheKeys(@PathVariable String cacheName) {
        return ApiResponse.<Set<String>>builder()
                .success(true)
                .message("Cache keys retrieved successfully")
                .data(cacheService.getKeys(cacheName))
                .build();
    }

    @DeleteMapping("/{cacheName}/clear")
    public ApiResponse<Void> clearCache(@PathVariable String cacheName) {
        cacheService.clear(cacheName);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Cache cleared successfully")
                .build();
    }

    @DeleteMapping("/{cacheName}/keys/{key}")
    public ApiResponse<Boolean> evictCacheKey(@PathVariable String cacheName, @PathVariable String key) {
        boolean result = cacheService.evict(cacheName, key);
        return ApiResponse.<Boolean>builder()
                .success(result)
                .message(result ? "Cache key evicted successfully" : "Cache key not found")
                .data(result)
                .build();
    }
}