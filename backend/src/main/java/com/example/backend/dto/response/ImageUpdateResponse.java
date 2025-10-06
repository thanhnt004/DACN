package com.example.backend.dto.response;

import com.example.backend.config.CloudinaryProps;
import com.example.backend.config.UploadConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ImageUpdateResponse {
    private String cloudName;
    private String apiKey;
    private String uploadUrl;
    private long timestamp;
    private String signature;
    private String folder;
    private long maxSize;
    private List<String> allowedFormat;
    private String transformations;
    public static ImageUpdateResponse create(CloudinaryProps cloudinaryConfig, UploadConfig uploadConfig, String signature, String folder)
    {
        long timestamp = System.currentTimeMillis();
        return new ImageUpdateResponse(cloudinaryConfig.getCloudName(), cloudinaryConfig.getApiKey(), cloudinaryConfig.getUploadUrl(),
                timestamp,signature,folder
                ,uploadConfig.maxSize(),uploadConfig.allowedFormats(),uploadConfig.transformations());
    }
}
