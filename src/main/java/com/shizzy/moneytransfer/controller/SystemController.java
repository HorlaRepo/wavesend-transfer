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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

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

    @GetMapping("/redis-debug")
    public ResponseEntity<Map<String, Object>> debugRedis() {
        Map<String, Object> results = new HashMap<>();

        // Configuration
        boolean sslEnabled = false;
        try {
            Object factory = redisTemplate.getConnectionFactory();
            // LettuceConnectionFactory supports getClientConfiguration().isUseSsl()
            if (factory != null && factory.getClass().getName().contains("LettuceConnectionFactory")) {
                // Use reflection to avoid direct dependency
                java.lang.reflect.Method getClientConfig = factory.getClass().getMethod("getClientConfiguration");
                Object clientConfig = getClientConfig.invoke(factory);
                java.lang.reflect.Method isUseSsl = clientConfig.getClass().getMethod("isUseSsl");
                sslEnabled = (Boolean) isUseSsl.invoke(clientConfig);
            }
        } catch (Exception ex) {
            logger.warn("Could not determine SSL status for Redis: {}", ex.getMessage());
        }
        results.put("config", Map.of(
                "redis_host", redisHost,
                "redis_port", redisPort,
                "redis_password_set", redisPassword != null && !redisPassword.isEmpty(),
                "ssl_enabled", sslEnabled));

        // 1. Try socket connection first
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(redisHost, redisPort), 3000);
            results.put("socket_connected", socket.isConnected());
        } catch (Exception e) {
            results.put("socket_error", e.getMessage());
        }

        // 2. Try raw Redis protocol
        try (Socket socket = new Socket(redisHost, redisPort);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream()) {

            // Send PING command
            out.write("*1\r\n$4\r\nPING\r\n".getBytes());
            out.flush();

            // Read response
            byte[] buffer = new byte[1024];
            int bytesRead = in.read(buffer);
            String response = new String(buffer, 0, bytesRead);

            results.put("raw_ping", response);
        } catch (Exception e) {
            results.put("raw_ping_error", e.getMessage());
        }

        // 3. Try with Redis client
        try {
            RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();
            String pong = new String(conn.ping());
            results.put("client_ping", pong);
        } catch (Exception e) {
            results.put("client_error", e.getMessage());
            results.put("client_error_cause", e.getCause() != null ? e.getCause().getMessage() : "No cause");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            results.put("stack_trace", sw.toString());
        }

        return ResponseEntity.ok(results);
    }
}