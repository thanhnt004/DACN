package com.example.backend.controller.catalog.product;

import com.example.backend.dto.request.catalog.product.InventoryRequest;
import com.example.backend.dto.response.catalog.product.InventoryResponse;
import com.example.backend.service.product.ProductInventoryService;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize(value = "hasRole('ADMIN') or hasRole('STAFF')")
public class ProductInventoryController {
    private final ProductInventoryService productInventoryService;
    @PostMapping("/variants/{variantId}/inventory")
    public ResponseEntity<InventoryResponse> create(@PathVariable UUID variantId,
                                                    @RequestBody InventoryRequest request){
        return ResponseEntity.ok(productInventoryService.create(variantId,request));
    }
    @PutMapping("/variants/{variantId}/inventory")
    public ResponseEntity<InventoryResponse> update(@PathVariable UUID variantId,
                                                    @RequestBody InventoryRequest request){
        return ResponseEntity.ok(productInventoryService.update(variantId,request));
    }
    @GetMapping("/variants/{variantId}/inventory")
    public ResponseEntity<InventoryResponse> getByVariantId(@PathVariable UUID variantId)
    {
        return ResponseEntity.ok(productInventoryService.get(variantId));
    }
}
