package com.example.backend.controller.product;

import com.example.backend.dto.ProductImageDto;
import com.example.backend.dto.request.product.ProductCreateRequest;
import com.example.backend.dto.request.product.ProductUpdateRequest;
import com.example.backend.dto.response.product.ProductResponse;
import com.example.backend.model.product.ProductStatus;
import com.example.backend.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@PreAuthorize(value = "hasRole('ADMIN') or hasRole('STAFF')")
public class AdminProductController {
    private final ProductService productService;
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.create(request);
        URI location = URI.create("/api/v1/products/"+response.getId());
        return ResponseEntity.created(location).body(response);
    }
    @PutMapping(value = "/{id}")
    public ResponseEntity<ProductResponse> update(@RequestBody ProductUpdateRequest request)
    {
        ProductResponse response = productService.update(request);
        return ResponseEntity.ok(response);
    }
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id)
    {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping(value = "/{id}")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID id,
            @RequestParam("status") ProductStatus status)
    {
        productService.changeStatus(id,status);
        return ResponseEntity.noContent().build();
    }
    //image
    @PostMapping(value = "/{id}/images")
    public ResponseEntity<?> addImage(@PathVariable("id") UUID productId,@RequestBody ProductImageDto dto)
    {
        productService.addImage(productId,dto);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping(value = "/{id}/images/{imageId}")
    public ResponseEntity<?> deleteImage(@PathVariable("id") UUID productId,
                                         @PathVariable("imageId") UUID imageId
    ){
        productService.removeImage(productId, imageId);
        return ResponseEntity.noContent().build();
    }
}
