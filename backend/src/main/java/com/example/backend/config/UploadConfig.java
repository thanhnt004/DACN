package com.example.backend.config;

import java.util.List;

public record UploadConfig(
        String folderPattern,
        long maxSize,
        List<String> allowedFormats,
        String transformations
) {}