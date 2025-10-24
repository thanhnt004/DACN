package com.example.backend.controller.catalog.product;

import com.example.backend.dto.request.catalog.product.ProductCreateRequest;
import com.example.backend.dto.request.catalog.product.ProductUpdateRequest;
import com.example.backend.dto.response.catalog.product.ProductDetailResponse;
import com.example.backend.model.product.ProductStatus;
import com.example.backend.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize(value = "hasRole('ADMIN') or hasRole('STAFF')")
public class AdminProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDetailResponse> createProduct(@RequestBody ProductCreateRequest request) {
        ProductDetailResponse response = productService.create(request);
        URI location = URI.create("/api/v1/products/"+response.getId());
        return ResponseEntity.created(location).body(response);
    }
    @PutMapping(value = "/{id}")
    public ResponseEntity<ProductDetailResponse> update(@RequestBody ProductUpdateRequest request)
    {
        ProductDetailResponse response = productService.update(request);
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
}
