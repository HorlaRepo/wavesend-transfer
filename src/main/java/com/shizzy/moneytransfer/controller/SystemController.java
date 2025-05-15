package com.shizzy.moneytransfer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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

    @GetMapping("/redis-ping")
    public ResponseEntity<String> pingRedis() {
        try {
            RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();
            String pong = new String(conn.ping());
            return ResponseEntity.ok("Redis replied: " + pong);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error connecting to Redis: " + e.getMessage());
        }
    }

    @GetMapping("/redis-test")
    public ResponseEntity<Map<String, Object>> testRedis() {
        Map<String, Object> results = new HashMap<>();

        // Add configuration info
        results.put("config", Map.of(
                "redis_host", redisHost,
                "redis_port", redisPort,
                "kafka_servers", kafkaServers));

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

    @GetMapping("/network-test")
    public ResponseEntity<Map<String, Object>> testNetwork() {
        Map<String, Object> results = new HashMap<>();

        // Test Redis connection
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(redisHost, redisPort), 5000);
            results.put("socket_connected", socket.isConnected());
            socket.close();
        } catch (Exception e) {
            results.put("socket_error", e.getMessage());
        }

        // Try DNS resolution
        try {
            InetAddress address = InetAddress.getByName(redisHost);
            results.put("dns_lookup", address.getHostAddress());
        } catch (Exception e) {
            results.put("dns_error", e.getMessage());
        }

        // Add network context
        try {
            results.put("local_ip", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            results.put("local_ip_error", e.getMessage());
        }

        return ResponseEntity.ok(results);
    }
}