package com.example.backend.controller.product;

import com.example.backend.dto.response.product.ProductResponse;
import com.example.backend.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    @GetMapping(value = "/{id}")
    public ResponseEntity<ProductResponse> findById(
            @PathVariable UUID id,
            @RequestParam(required = false) List<String> include
    )
    {
        ProductResponse response =  productService.findById(id,include);
        return ResponseEntity.ok(response);
    }
}
