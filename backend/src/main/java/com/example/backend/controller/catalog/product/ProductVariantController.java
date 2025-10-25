package com.example.backend.controller.catalog.product;

import com.example.backend.dto.request.catalog.product.VariantCreateRequest;
import com.example.backend.dto.request.catalog.product.VariantUpdateRequest;
import com.example.backend.dto.response.catalog.product.VariantResponse;
import com.example.backend.service.product.ProductVariantService;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

@RestController
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
                                                            @Valid @RequestBody VariantCreateRequest request
                                                            ) throws URISyntaxException {
        VariantResponse response = productVariantService.addVariant(request,productId);
        URI location = new URI("/api/v1/admin/products/"+response.getProductId()+"/variants/"+response.getId());
        return ResponseEntity.created(location).body(response);
    }
    @PutMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<VariantResponse> update(@PathVariable UUID productId,
                                                  @PathVariable UUID variantId,
                                                  @Valid @RequestBody VariantUpdateRequest request
    ){
        return ResponseEntity.ok(productVariantService.update(productId,variantId,request));
    }
    @DeleteMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<?> delete(@PathVariable UUID productId,
                                                  @PathVariable UUID variantId
    ){
        productVariantService.deleteVariant(variantId,productId);
        return ResponseEntity.noContent().build();
    }
}
