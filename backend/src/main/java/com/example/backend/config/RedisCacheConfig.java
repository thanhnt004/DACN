package com.example.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration
 * Cấu hình cache với Redis backend, tự động serialize/deserialize objects
 */
@Configuration
@RequiredArgsConstructor
public class RedisCacheConfig implements CachingConfigurer {

    private final CacheConfig cacheConfig;

    /**
     * Cấu hình ObjectMapper cho Redis serialization (PRIVATE - chỉ dùng cho cache)
     * Hỗ trợ Java 8 Time API và polymorphic types
     * KHÔNG expose như @Bean để tránh conflict với HTTP message converters
     */
    private ObjectMapper createRedisCacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Hỗ trợ Java 8 Time API (LocalDateTime, Instant, etc.)
        mapper.registerModule(new JavaTimeModule());

        // Cấu hình polymorphic type handling để deserialize đúng class
        // CHỈ dùng cho Redis cache, KHÔNG ảnh hưởng HTTP requests
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        return mapper;
    }

    /**
     * Cấu hình CacheManager với Redis
     * Định nghĩa các cache khác nhau với TTL khác nhau
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        
        // Debug logging
        System.out.println("=== Cache Configuration Debug ===");
        System.out.println("Product Cache: " + cacheConfig.getProductCache());
        System.out.println("Category Cache: " + cacheConfig.getCategoryCache());
        System.out.println("User Cache: " + cacheConfig.getUserCache());
        System.out.println("Brand Cache: " + cacheConfig.getBrandCache());
        System.out.println("=================================");

        // Cấu hình serializer với private ObjectMapper (không ảnh hưởng HTTP)
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(createRedisCacheObjectMapper());

        // Cấu hình mặc định cho tất cả cache
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // TTL mặc định: 1 giờ
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues(); // Không cache giá trị null

        // Cấu hình riêng cho từng cache với TTL khác nhau
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Helper method để thêm cache configuration với null check
        addCacheConfig(cacheConfigurations, cacheConfig.getProductCache(),
                defaultConfig.entryTtl(Duration.ofMinutes(30)));
        addCacheConfig(cacheConfigurations, cacheConfig.getProductListCache(),
                defaultConfig.entryTtl(Duration.ofMinutes(30)));
        addCacheConfig(cacheConfigurations, cacheConfig.getProductVariantsCache(),
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Categories - 2 giờ (ít thay đổi)
        addCacheConfig(cacheConfigurations, cacheConfig.getCategoryCache(),
                defaultConfig.entryTtl(Duration.ofHours(2)));

        // Users - 15 phút (thông tin nhạy cảm)
        addCacheConfig(cacheConfigurations, cacheConfig.getUserCache(),
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Provinces/Districts/Wards - 24 giờ (static data)
        addCacheConfig(cacheConfigurations, cacheConfig.getProvinceCache(),
                defaultConfig.entryTtl(Duration.ofHours(24)));
        addCacheConfig(cacheConfigurations, cacheConfig.getDistrictCache(),
                defaultConfig.entryTtl(Duration.ofHours(24)));
        addCacheConfig(cacheConfigurations, cacheConfig.getWardCache(),
                defaultConfig.entryTtl(Duration.ofHours(24)));

        // Orders - 10 phút (cần fresh data)
        addCacheConfig(cacheConfigurations, cacheConfig.getOrderCache(),
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Banners - 1 giờ
        addCacheConfig(cacheConfigurations, cacheConfig.getBannerCache(),
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // Brands - 2 giờ
        addCacheConfig(cacheConfigurations, cacheConfig.getBrandCache(),
                defaultConfig.entryTtl(Duration.ofHours(2)));

        // Discounts - 15 phút (cần cập nhật nhanh)
        addCacheConfig(cacheConfigurations, cacheConfig.getDiscountCache(),
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Cart - 30 phút
        addCacheConfig(cacheConfigurations, cacheConfig.getCartCache(),
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Shipping - 1 giờ
        addCacheConfig(cacheConfigurations, cacheConfig.getShippingCache(),
                defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware() // Hỗ trợ transaction
                .build();
    }


    /**
     * Helper method để thêm cache configuration với null check
     */
    private void addCacheConfig(Map<String, RedisCacheConfiguration> cacheConfigurations, 
                                 String cacheName, 
                                 RedisCacheConfiguration config) {
        if (cacheName != null && !cacheName.isEmpty()) {
            cacheConfigurations.put(cacheName, config);
            System.out.println("Added cache: " + cacheName);
        } else {
            System.err.println("WARNING: Skipped null or empty cache name!");
        }
    }

    /**
     * Custom KeyGenerator cho cache
     * Tạo cache key từ class name, method name và parameters
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName()).append(".");
            sb.append(method.getName()).append(":");

            for (Object param : params) {
                if (param != null) {
                    sb.append(param.toString()).append("_");
                }
            }

            return sb.toString();
        };
    }

    /**
     * Custom CacheErrorHandler
     * Log errors nhưng không throw exception khi cache fail
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // Log error nhưng không throw - fail gracefully
                System.err.println("Cache GET error: " + exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                System.err.println("Cache PUT error: " + exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                System.err.println("Cache EVICT error: " + exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                System.err.println("Cache CLEAR error: " + exception.getMessage());
            }
        };
    }
}

