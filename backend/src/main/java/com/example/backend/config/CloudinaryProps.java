package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cloudinary")
@Data
public class CloudinaryProps {
    private String cloudName;
    private String apiKey;
    private String apiSecret;
    private String uploadUrl;
}
