package com.example.backend.controller.catalog.product;

import com.example.backend.dto.response.catalog.product.ProductDetailResponse;
import com.example.backend.dto.response.catalog.product.ProductSummaryResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    @GetMapping
    public ResponseEntity<PageResponse<ProductSummaryResponse>> get(
            @ModelAttribute ProductFilter filter,
            @PageableDefault(page = 0,size = 20)Pageable pageable
            )
    {
        PageResponse<ProductSummaryResponse> response =  productService.list(filter,pageable);
        return ResponseEntity.ok(response);
    }
    @GetMapping(value = "/{slugOrId}")
    public ResponseEntity<ProductDetailResponse> findBySlugOrId(@PathVariable String slugOrId, @RequestParam List<String> includes)
    {
        return ResponseEntity.ok(productService.findBySlugOrId(slugOrId,includes));
    }
}
