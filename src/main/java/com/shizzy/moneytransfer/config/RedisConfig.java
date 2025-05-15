package com.shizzy.moneytransfer.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shizzy.moneytransfer.util.CacheNames;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.protocol.ProtocolVersion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${cache.config.entryTtl:60}")
    private int entryTtl;

    @Value("${cache.config.transactions.entryTtl:60}")
    private int transactionsTtl;

    @Value("${cache.config.wallets.entryTtl:60}")
    private int walletsTtl;

    @Value("${cache.config.otp.entryTtl:60}")
    private int otpCacheTtl;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${spring.data.redis.timeout:2000}")
    private int timeout;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;


    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        try {
            logger.info("Configuring Redis for profile: {}", activeProfile);
            logger.info("Connecting to Redis at {}:{} with SSL: {}", redisHost, redisPort, sslEnabled);

            // Build client configuration with INCREASED timeouts
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(5))
                    .shutdownTimeout(Duration.ofSeconds(5))
                    .clientOptions(ClientOptions.builder()
                            .socketOptions(SocketOptions.builder()
                                    .connectTimeout(Duration.ofSeconds(5))
                                    .build())
                            .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5)))
                            // Disable CLIENT SETINFO which isn't supported
                            .protocolVersion(ProtocolVersion.RESP2)
                            .build())
                    // Don't set client name to avoid CLIENT commands
                    .build();

            // Use same Redis configuration
            RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
            redisConfig.setHostName(redisHost);
            redisConfig.setPort(redisPort);

            // Password logic remains the same
            if (redisPassword != null && !redisPassword.isEmpty()) {
                redisConfig.setPassword(redisPassword);
            }

            return new LettuceConnectionFactory(redisConfig, clientConfig);
        } catch (Exception e) {
            logger.error("Failed to configure Redis connection: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create a specialized ObjectMapper for Redis that includes type information
     */
    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = objectMapper.copy();

        // Configure the ObjectMapper to include type information
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        return mapper;
    }

    /**
     * Create a consistent GenericJackson2JsonRedisSerializer using our configured
     * redisObjectMapper
     */
    @Bean
    public GenericJackson2JsonRedisSerializer redisSerializer(
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    /**
     * Redis template configuration with consistent serialization
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            LettuceConnectionFactory lettuceConnectionFactory,
            GenericJackson2JsonRedisSerializer redisSerializer) {

        final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);

        // Use StringRedisSerializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);

        // Use our consistent serializer for values
        redisTemplate.setValueSerializer(redisSerializer);
        redisTemplate.setHashValueSerializer(redisSerializer);

        logger.info("Configured RedisTemplate with consistent serializers");
        return redisTemplate;
    }

    /**
     * Default Redis cache configuration
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(GenericJackson2JsonRedisSerializer redisSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(entryTtl))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(redisSerializer));
    }

    /**
     * Cache manager configuration
     */
    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            RedisCacheConfiguration redisCacheConfiguration) {

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .cacheWriter(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory))
                .transactionAware()
                .build();
    }

    /**
     * Configure different cache settings for different caches
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
            GenericJackson2JsonRedisSerializer redisSerializer) {

        return builder -> {
            Map<String, RedisCacheConfiguration> configMap = new HashMap<>();

            // Base configuration with consistent serializer
            RedisCacheConfiguration baseConf = RedisCacheConfiguration.defaultCacheConfig()
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(redisSerializer));

            // Transactions cache configuration
            RedisCacheConfiguration transactionsConf = baseConf.entryTtl(Duration.ofMinutes(transactionsTtl));

            // Wallets cache configuration
            RedisCacheConfiguration walletsConf = baseConf.entryTtl(Duration.ofMinutes(walletsTtl));

            // OTP cache configuration - short TTL
            RedisCacheConfiguration otpConf = baseConf.entryTtl(Duration.ofMinutes(otpCacheTtl));

            // Beneficiary cache configuration - medium TTL
            RedisCacheConfiguration beneficiaryConf = baseConf.entryTtl(Duration.ofMinutes(30));

            // Scheduled Transfer cache configuration - medium TTL
            RedisCacheConfiguration scheduledTransfersConf = baseConf.entryTtl(Duration.ofMinutes(30));

            // Add all cache configurations
            configMap.put(CacheNames.TRANSACTIONS, transactionsConf);
            configMap.put(CacheNames.SINGLE_TRANSACTION, transactionsConf);
            configMap.put(CacheNames.ALL_USER_TRANSACTION, transactionsConf);
            configMap.put(CacheNames.SEARCH_RESULT, transactionsConf);
            configMap.put(CacheNames.WALLETS, walletsConf);
            configMap.put("otpCache", otpConf);
            configMap.put("pending_transfers", otpConf);
            configMap.put("transfer_requests", otpConf);

            configMap.put(CacheNames.USER_BENEFICIARIES, beneficiaryConf);
            configMap.put(CacheNames.SINGLE_BENEFICIARY, beneficiaryConf);

            configMap.put(CacheNames.SCHEDULED_TRANSFERS, scheduledTransfersConf);
            configMap.put(CacheNames.SINGLE_SCHEDULED_TRANSFER, scheduledTransfersConf);
            configMap.put(CacheNames.USER_SCHEDULED_TRANSFERS, scheduledTransfersConf);
            configMap.put(CacheNames.RECURRING_SERIES, scheduledTransfersConf);

            builder.withInitialCacheConfigurations(configMap);
        };
    }

    @Bean("transactionKeyGenerator")
    public KeyGenerator transactionKeyGenerator() {
        return (Object target, Method method, Object... params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName()).append(":");
            sb.append(method.getName()).append(":");

            for (Object param : params) {
                sb.append(param).append(":");
            }

            String key = sb.toString();
            logger.debug("Generated cache key: {}", key);
            return key;
        };
    }

    @Bean
    public RedisCacheErrorHandler errorHandler() {
        return new RedisCacheErrorHandler();
    }
}