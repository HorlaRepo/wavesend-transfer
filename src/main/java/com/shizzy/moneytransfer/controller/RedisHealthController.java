package com.shizzy.moneytransfer.controller;

import java.sql.Connection;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("health")
@RequiredArgsConstructor
public class RedisHealthController {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(RedisHealthController.class);
    private final DataSource dataSource;


    
    /**
     * Endpoint to check the health of the Redis server.
     *
     * @return a ResponseEntity containing the health status of the Redis server.
     */
    @GetMapping("/redis")
    public ResponseEntity<Map<String, String>> redisHealth() {
        Map<String, String> response = new HashMap<>();
        try {
            // Attempt a simple Redis operation
            redisTemplate.opsForValue().set("health:test", "OK", Duration.ofSeconds(10));
            String result = (String) redisTemplate.opsForValue().get("health:test");
            if ("OK".equals(result)) {
                response.put("status", "UP");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "DOWN");
                response.put("reason", "Test key value mismatch");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("reason", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }


    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        
        Map<String, String> services = new HashMap<>();
        
        // Check Redis
        try {
            redisTemplate.getConnectionFactory().getConnection();
            services.put("redis", "UP");
        } catch (Exception e) {
            logger.error("Redis health check failed: {}", e.getMessage());
            services.put("redis", "DOWN: " + e.getMessage());
            health.put("status", "DEGRADED");
        }
        
        // Check Database
        try (Connection conn = dataSource.getConnection()) {
            conn.isValid(3);
            services.put("database", "UP");
        } catch (Exception e) {
            logger.error("Database health check failed: {}", e.getMessage());
            services.put("database", "DOWN: " + e.getMessage());
            health.put("status", "DEGRADED");
        }
        
        health.put("services", services);
        return ResponseEntity.ok(health);
    }
}
