package com.example.backend.config;

import com.cloudinary.Cloudinary;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@AllArgsConstructor
public class CloudinaryConfig {

    private CloudinaryProps cloudinaryProps;

    @Bean
    public Cloudinary cloudinary() {
        Map<String,String> config = new HashMap();
        config.put("cloud_name", cloudinaryProps.getCloudName());
        config.put("api_key", cloudinaryProps.getApiKey());
        config.put("api_secret", cloudinaryProps.getApiSecret());
        return new Cloudinary(config);

    }
}
