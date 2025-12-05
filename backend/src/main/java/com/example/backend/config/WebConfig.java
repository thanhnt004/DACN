package com.example.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Web Configuration
 * Cấu hình ObjectMapper cho HTTP requests/responses
 * KHÁC với ObjectMapper dùng cho Redis cache
 */
@Configuration
public class WebConfig {

    /**
     * ObjectMapper chính cho HTTP message conversion
     * @Primary để Spring ưu tiên dùng bean này cho HTTP
     * KHÔNG có polymorphic type handling để tránh yêu cầu @class property
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .build();
    }
}

