package com.shizzy.moneytransfer.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.shizzy.moneytransfer.util.CacheNames;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
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

        // Inject the ObjectMapper from JacksonConfig
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

        @Bean
        public LettuceConnectionFactory redisConnectionFactory() {
                try {
                        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
                        redisConfig.setHostName(redisHost);
                        redisConfig.setPort(redisPort);

                        if (redisPassword != null && !redisPassword.isEmpty()) {
                                redisConfig.setPassword(redisPassword);
                        }

                        SocketOptions socketOptions = SocketOptions.builder()
                                        .connectTimeout(Duration.ofMillis(timeout))
                                        .build();

                        ClientOptions clientOptions = ClientOptions.builder()
                                        .timeoutOptions(TimeoutOptions.enabled())
                                        .socketOptions(socketOptions)
                                        .build();

                        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = LettuceClientConfiguration
                                        .builder()
                                        .commandTimeout(Duration.ofMillis(timeout))
                                        .clientOptions(clientOptions);

                        if (sslEnabled) {
                                logger.info("Enabling SSL for Redis connection");
                                clientConfigBuilder.useSsl();
                        }

                        return new LettuceConnectionFactory(redisConfig, clientConfigBuilder.build());
                } catch (Exception e) {
                        logger.error("Failed to configure Redis connection: {}", e.getMessage(), e);
                        throw e;
                }
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory,
                        ObjectMapper objectMapper) {
                final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
                redisTemplate.setConnectionFactory(lettuceConnectionFactory);
                // value serializer
                redisTemplate.setKeySerializer(new StringRedisSerializer());
                redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
                // hash value serializer
                redisTemplate.setHashKeySerializer(new StringRedisSerializer());
                redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

                logger.info("wiring up Redistemplate...");
                return redisTemplate;
        }

        @Bean
        public RedisCacheConfiguration redisCacheConfiguration() {
                // Create a mapper with type information
                ObjectMapper redisMapper = objectMapper.copy();
                redisMapper.activateDefaultTyping(
                                LaissezFaireSubTypeValidator.instance,
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                JsonTypeInfo.As.PROPERTY);

                GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisMapper);

                return RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(entryTtl))
                                .disableCachingNullValues()
                                .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(serializer));
        }

        /**
         * Configure ObjectMapper for Redis serialization
         */
        private ObjectMapper redisObjectMapper() {
                ObjectMapper mapper = objectMapper.copy();
                mapper.activateDefaultTyping(
                                LaissezFaireSubTypeValidator.instance,
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                JsonTypeInfo.As.PROPERTY);
                return mapper;
        }

        @Bean
        public Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer() {
                Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(redisObjectMapper(),
                                Object.class);

                // Set a breakpoint here during debug to inspect the serializer
                return serializer;
        }

        @Bean
        public RedisCacheErrorHandler errorHandler() {
                return new RedisCacheErrorHandler();
        }

        @Bean
        public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {

                ObjectMapper mapper = objectMapper.copy();
                mapper.activateDefaultTyping(
                                mapper.getPolymorphicTypeValidator(),
                                ObjectMapper.DefaultTyping.NON_FINAL);
                // Use the injected ObjectMapper
                GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

                return builder -> {
                        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();

                        // Transactions cache configuration
                        RedisCacheConfiguration transactionsConf = RedisCacheConfiguration.defaultCacheConfig()
                                        .entryTtl(Duration.ofMinutes(transactionsTtl))
                                        .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(new StringRedisSerializer()))
                                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(serializer));

                        // Wallets cache configuration
                        RedisCacheConfiguration walletsConf = RedisCacheConfiguration.defaultCacheConfig()
                                        .entryTtl(Duration.ofMinutes(walletsTtl))
                                        .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(new StringRedisSerializer()))
                                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(serializer));

                        // OTP cache configuration
                        RedisCacheConfiguration otpConf = RedisCacheConfiguration.defaultCacheConfig()
                                        .entryTtl(Duration.ofMinutes(otpCacheTtl))
                                        .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(new StringRedisSerializer()))
                                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(serializer));

                        configMap.put(CacheNames.TRANSACTIONS, transactionsConf);
                        configMap.put(CacheNames.SINGLE_TRANSACTION, transactionsConf);
                        configMap.put(CacheNames.ALL_USER_TRANSACTION, transactionsConf);
                        configMap.put(CacheNames.SEARCH_RESULT, transactionsConf);
                        configMap.put(CacheNames.WALLETS, walletsConf);
                        configMap.put("otpCache", otpConf);

                        builder.withInitialCacheConfigurations(configMap);

                        // Beneficiary cache configuration
                        RedisCacheConfiguration beneficiaryConf = RedisCacheConfiguration.defaultCacheConfig()
                                        .entryTtl(Duration.ofMinutes(30)) // Cache for 30 minutes
                                        .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(new StringRedisSerializer()))
                                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(serializer));

                        configMap.put(CacheNames.USER_BENEFICIARIES, beneficiaryConf);
                        configMap.put(CacheNames.SINGLE_BENEFICIARY, beneficiaryConf);

                        // Scheduled Transfer cache configuration
                        RedisCacheConfiguration scheduledTransfersConf = RedisCacheConfiguration.defaultCacheConfig()
                                        .entryTtl(Duration.ofMinutes(30)) // Cache for 30 minutes
                                        .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(new StringRedisSerializer()))
                                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(serializer));

                        configMap.put(CacheNames.SCHEDULED_TRANSFERS, scheduledTransfersConf);
                        configMap.put(CacheNames.SINGLE_SCHEDULED_TRANSFER, scheduledTransfersConf);
                        configMap.put(CacheNames.USER_SCHEDULED_TRANSFERS, scheduledTransfersConf);
                        configMap.put(CacheNames.RECURRING_SERIES, scheduledTransfersConf);

                        builder.withInitialCacheConfigurations(configMap);
                };
        }

        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(redisCacheConfiguration())
                                .cacheWriter(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory))
                                .transactionAware()
                                .build();
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
}