package com.example.backend.config;

import com.example.backend.dto.response.checkout.CheckoutSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, CheckoutSession> checkoutSessionRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, CheckoutSession> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Serializer cho key: String
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Serializer cho value: JSON với ObjectMapper tùy chỉnh
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
    return template;
    }
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();

        return template;
    }
    @Bean
    public RedisCacheConfiguration defaultCacheConfig() {
        // Serializer cho key và value
        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        RedisSerializer<Object> valueSerializer = new GenericJackson2JsonRedisSerializer();

        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .entryTtl(Duration.ofMinutes(10))    // TTL mặc định
                .disableCachingNullValues()
                .prefixCacheNameWith("myapp::");     // namespace cho tránh collision
    }
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // per-cache TTL example
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("users", defaultCacheConfig().entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("short-lived", defaultCacheConfig().entryTtl(Duration.ofSeconds(60)));
        cacheConfigs.put("products", defaultCacheConfig().entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("checkout:sessions", defaultCacheConfig().entryTtl(Duration.ofMinutes(60)));

        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory))
                .cacheDefaults(defaultCacheConfig())
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
