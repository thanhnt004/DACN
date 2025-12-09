package com.example.backend.controller.catalog.product;

import com.example.backend.dto.response.catalog.product.ProductSearchResult;
import com.example.backend.dto.response.catalog.product.ProductSummaryResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.service.product.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for product search using vector embeddings
 */
@RestController
@RequestMapping("/api/v1/products/search")
@RequiredArgsConstructor
@Tag(name = "Product Search", description = "Vector-based product search endpoints")
public class ProductSearchController {

    private final ProductSearchService searchService;

    @GetMapping("/vector")
    @Operation(summary = "Vector search for products",
               description = "Search products using vector embeddings with similarity scores")
    public ResponseEntity<List<ProductSearchResult>> vectorSearch(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Maximum number of results") @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "Minimum similarity threshold") @RequestParam(required = false) Double threshold) {
        return ResponseEntity.ok(searchService.vectorSearch(query, limit, threshold));
    }

    @GetMapping("/suggest")
    @Operation(summary = "Get search suggestions",
               description = "Get product suggestions based on query")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Maximum number of suggestions") @RequestParam(defaultValue = "5") Integer limit) {
        return ResponseEntity.ok(searchService.getSearchSuggestions(query, limit));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get search statistics",
               description = "Get statistics about indexed products")
    public ResponseEntity<Map<String, Object>> getSearchStats() {
        return ResponseEntity.ok(searchService.getSearchStats());
    }
}

