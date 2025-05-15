package com.shizzy.moneytransfer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/system")
public class SystemController {
    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.kafka.bootstrap-servers:not-configured}")
    private String kafkaServers;
    
    public SystemController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @GetMapping("/redis-test")
    public ResponseEntity<Map<String, Object>> testRedis() {
        Map<String, Object> results = new HashMap<>();
        
        // Add configuration info
        results.put("config", Map.of(
            "redis_host", redisHost,
            "redis_port", redisPort,
            "kafka_servers", kafkaServers
        ));
        
        try {
            // Create test key with timestamp to avoid conflicts
            String testKey = "redis-test-" + System.currentTimeMillis();
            String testValue = "Test at " + LocalDateTime.now();
            
            // Write to Redis
            redisTemplate.opsForValue().set(testKey, testValue, Duration.ofMinutes(5));
            
            // Read from Redis
            Object retrievedValue = redisTemplate.opsForValue().get(testKey);
            
            // Add success info to response
            results.put("success", true);
            results.put("wrote_value", testValue);
            results.put("read_value", retrievedValue);
            
            logger.info("Redis test successful: {}", retrievedValue);
        } catch (Exception e) {
            // Add failure info to response
            results.put("success", false);
            results.put("error_type", e.getClass().getName());
            results.put("error_message", e.getMessage());
            
            logger.error("Redis test failed: {}", e.getMessage(), e);
        }
        
        return ResponseEntity.ok(results);
    }
}