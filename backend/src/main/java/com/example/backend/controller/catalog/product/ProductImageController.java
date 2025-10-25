package com.example.backend.controller.catalog.product;

import com.example.backend.dto.request.catalog.product.ProductImageRequest;
import com.example.backend.dto.response.catalog.product.ProductImageResponse;
import com.example.backend.service.product.ProductImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize(value = "hasRole('ADMIN') or hasRole('STAFF')")
public class ProductImageController {
    private final ProductImageService productImageService;
    @PostMapping(value = "/{id}/images")
    public ResponseEntity<?> addImage(@PathVariable("id") UUID productId,@Valid @RequestBody ProductImageRequest dto)
    {
        productImageService.addImage(productId,dto);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping(value = "/{id}/images/{imageId}")
    public ResponseEntity<?> deleteImage(@PathVariable("id") UUID productId,
                                         @PathVariable("imageId") UUID imageId
    ){
        productImageService.removeImage(productId, imageId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{productId}/images")
    public ResponseEntity<List<ProductImageResponse>> getImages(@PathVariable UUID productId)
    {
        return ResponseEntity.ok(productImageService.getImages(productId));
    }
    @PutMapping("/{productId}/images/{imageId}")
    public ResponseEntity<?> updateImage(@PathVariable("productId") UUID productId,
                                         @PathVariable("imageId") UUID imageId,
                                         @Valid @RequestBody ProductImageRequest request
    )
    {
        productImageService.updateImage(productId,imageId,request);
        return ResponseEntity.status(200).build();
    }
}
