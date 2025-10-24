package com.example.backend.controller.common;

import com.example.backend.dto.request.common.ImageUploadRequest;
import com.example.backend.dto.response.common.ImageUploadResponse;
import com.example.backend.service.product.CloudImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/cloudinary")
@RestController
@RequiredArgsConstructor
public class CloudinarySignController {
    private final CloudImageService service;
    @PostMapping("/sign")
    public ResponseEntity<ImageUploadResponse> sign(@RequestBody @Valid ImageUploadRequest request)
    {
        return ResponseEntity.ok(service.sign(request));
    }

}
