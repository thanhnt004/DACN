package com.example.backend.service.product;

import com.example.backend.dto.response.catalog.product.ProductSearchResult;
import com.example.backend.dto.response.catalog.product.ProductSummaryResponse;
import com.example.backend.entity.ProductEmbedding;
import com.example.backend.model.product.Product;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.service.ProductEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for product search using vector embeddings
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSearchService {

    private final ProductEmbeddingService embeddingService;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Value("${vector.database.similarity-threshold:0.7}")
    private Double defaultSimilarityThreshold;

    /**
     * Perform vector search and return results with similarity scores
     */
    public List<ProductSearchResult> vectorSearch(String query, Integer limit, Double threshold) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Double effectiveThreshold = threshold != null ? threshold : defaultSimilarityThreshold;

        try {
            // Get similar products from embedding service
            List<ProductEmbedding> embeddings = embeddingService.searchSimilarProducts(query, limit);

            if (embeddings.isEmpty()) {
                log.info("No similar products found for query: {}", query);
                return Collections.emptyList();
            }

            // Extract product IDs
            List<UUID> productIds = embeddings.stream()
                    .map(ProductEmbedding::getProductId)
                    .collect(Collectors.toList());

            // Fetch products
            List<Product> products = productRepository.findAllById(productIds);

            // Build response with similarity scores
            Map<UUID, Product> productMap = products.stream()
                    .collect(Collectors.toMap(
                            Product::getId,
                            p -> p
                    ));

            return embeddings.stream()
                    .filter(e -> productMap.containsKey(e.getProductId()))
                    .map(embedding -> {
                        Product product = productMap.get(embedding.getProductId());
                        return ProductSearchResult.builder()
                                .productId(product.getId())
                                .embeddingId(embedding.getId())
                                .content(embedding.getContent())
                                .similarity(calculateSimilarity(embedding))
                                .product(convertToSummary(product))
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error performing vector search for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get search suggestions based on query
     */
    public List<String> getSearchSuggestions(String query, Integer limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<ProductEmbedding> embeddings = embeddingService.searchSimilarProducts(query, limit);

            return embeddings.stream()
                    .map(ProductEmbedding::getContent)
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting search suggestions for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get search statistics
     */
    public Map<String, Object> getSearchStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            long embeddingCount = embeddingService.countEmbeddings();
            long productCount = productRepository.count();

            stats.put("totalProducts", productCount);
            stats.put("indexedProducts", embeddingCount);
            stats.put("indexedPercentage", productCount > 0 ?
                    (double) embeddingCount / productCount * 100 : 0);
            stats.put("similarityThreshold", defaultSimilarityThreshold);
            stats.put("status", embeddingCount > 0 ? "active" : "no_embeddings");

        } catch (Exception e) {
            log.error("Error getting search stats", e);
            stats.put("error", "Failed to retrieve statistics");
            stats.put("status", "error");
        }

        return stats;
    }

    private ProductSummaryResponse convertToSummary(Product product) {
        ProductSummaryResponse response = new ProductSummaryResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setSlug(product.getSlug());
        response.setPriceAmount(product.getPriceAmount());
        response.setImageUrl(product.getPrimaryImageUrl());
        response.setStatus(product.getStatus());
        response.setGender(String.valueOf(product.getGender()));
        
        // Check if product has any variant in stock and calculate total available stock
        int totalStock = 0;
        boolean inStock = false;
        if (product.getVariants() != null) {
            for (var v : product.getVariants()) {
                if (v.getInventory() != null && v.getInventory().getAvailableStock() > 0) {
                    inStock = true;
                    totalStock += v.getInventory().getAvailableStock();
                }
            }
        }
        response.setIsInStock(inStock);
        response.setTotalStock(totalStock);
        
        // Note: Colors and sizes would need to be fetched separately if needed
        return response;
    }

    private Double calculateSimilarity(ProductEmbedding embedding) {
        // The similarity is calculated in the SQL query and stored in the embedding
        return embedding.getSimilarity() != null ? embedding.getSimilarity() : 0.0;
    }
}

