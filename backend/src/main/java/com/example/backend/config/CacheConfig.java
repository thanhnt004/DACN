package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.cache.values")
@Data // Sử dụng @Data của Lombok để tự động tạo getter/setter
public class CacheConfig {
    // User related caches
    private String userCache;

    // Product related caches
    private String productCache;
    private String productVariantsCache = "productVariants";
    private String productListCache = "productList";

    // Category related caches
    private String categoryCache;

    // Brand cache
    private String brandCache = "brands";

    // Discount cache
    private String discountCache = "discounts";

    // Banner cache
    private String bannerCache;

    // Location caches (static data)
    private String provinceCache;
    private String districtCache;
    private String wardCache;

    // Order related caches
    private String orderCache;

    // Cart cache
    private String cartCache = "carts";

    // Shipping cache
    private String shippingCache = "shipping";
}
