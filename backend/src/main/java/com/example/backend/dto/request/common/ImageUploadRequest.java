package com.example.backend.dto.request.common;

import lombok.Data;

import java.util.UUID;

@Data
public class ImageUploadRequest {
    private String type;
    private UUID targetId;
}
