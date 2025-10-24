package com.example.backend.controller.catalog.product;

import com.example.backend.dto.request.catalog.product.VariantCreateRequest;
import com.example.backend.dto.request.catalog.product.VariantUpdateRequest;
import com.example.backend.dto.response.catalog.product.VariantResponse;
import com.example.backend.service.product.ProductVariantService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize(value = "hasRole('ADMIN') or hasRole('STAFF')")
public class ProductVariantController {
    private final ProductVariantService productVariantService;
    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<VariantResponse>> getList(@PathVariable UUID productId)
    {
        return ResponseEntity.ok(productVariantService.getList(productId));
    }
    @GetMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<VariantResponse> getById(@PathVariable UUID productId,@PathVariable UUID variantId)
    {
        return ResponseEntity.ok(productVariantService.getById(productId,variantId));
    }
    @PostMapping("/{productId}/variants")
    public ResponseEntity<VariantResponse> createNewVariant(@PathVariable UUID productId,
                                                            @RequestBody VariantCreateRequest request
                                                            ){
        return ResponseEntity.ok(productVariantService.addVariant(request,productId));
    }
    @PutMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<VariantResponse> update(@PathVariable UUID productId,
                                                  @PathVariable UUID variantId,
                                                  @RequestBody VariantUpdateRequest request
    ){
        return ResponseEntity.ok(productVariantService.update(productId,variantId,request));
    }
    @DeleteMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<?> delete(@PathVariable UUID productId,
                                                  @PathVariable UUID variantId,
                                                  @RequestBody VariantUpdateRequest request
    ){
        productVariantService.deleteVariant(variantId,productId);
        return ResponseEntity.noContent().build();
    }
}
