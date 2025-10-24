package com.example.backend.dto.response.common;

import com.example.backend.config.CloudinaryProps;
import com.example.backend.config.UploadConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ImageUploadResponse {
    private String cloudName;
    private String apiKey;
    private String uploadUrl;
    private long timestamp;
    private String signature;
    private String folder;
    private long maxSize;
    private List<String> allowedFormat;
    private String transformations;
    public static ImageUploadResponse create(CloudinaryProps cloudinaryConfig, UploadConfig uploadConfig, String signature, String folder)
    {
        long timestamp = System.currentTimeMillis();
        return new ImageUploadResponse(cloudinaryConfig.getCloudName(), cloudinaryConfig.getApiKey(), cloudinaryConfig.getUploadUrl(),
                timestamp,signature,folder
                ,uploadConfig.maxSize(),uploadConfig.allowedFormats(),uploadConfig.transformations());
    }
}
