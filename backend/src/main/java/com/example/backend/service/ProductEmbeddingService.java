package com.example.backend.service;

import com.example.backend.entity.ProductEmbedding;
import com.example.backend.repository.ProductEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductEmbeddingService {

    private final ProductEmbeddingRepository embeddingRepository;
    private final GeminiService geminiService;

    @Value("${vector.database.similarity-threshold:0.7}")
    private Double similarityThreshold;

    @Async("embeddingTaskExecutor")
    @Transactional
    public CompletableFuture<Void> generateProductEmbedding(UUID productId, String productContent) {
        log.info("Starting embedding generation for product ID: {}", productId);

        return geminiService.generateEmbedding(productContent)
                .map(this::convertToVectorString)
                .doOnNext(vectorString -> {
                    ProductEmbedding embedding = embeddingRepository.findByProductId(productId)
                            .orElse(new ProductEmbedding());

                    embedding.setProductId(productId);
                    embedding.setContent(productContent);
                    embedding.setEmbedding(vectorString);
                    embedding.setEmbeddingVersion(1);

                    embeddingRepository.save(embedding);
                    log.info("Successfully saved embedding for product ID: {}", productId);
                })
                .doOnError(error -> log.error("Failed to generate embedding for product ID: {}", productId, error))
                .then()
                .toFuture();
    }

    public List<ProductEmbedding> searchSimilarProducts(String query, Integer limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return geminiService.generateEmbedding(query)
                .map(embedding -> {
                    String queryVector = convertToVectorString(embedding);
                    List<Object[]> results = embeddingRepository.findSimilarProducts(
                            queryVector, similarityThreshold, limit != null ? limit : 10);

                    return results.stream()
                            .map(this::mapToProductEmbedding)
                            .toList();
                })
                .block();
    }

    /**
     * Search for product IDs with similarity scores
     * Returns Map of productId -> similarity score
     */
    public Map<UUID, Double> searchProductIdsBySimilarity(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<Double> embedding = geminiService.generateEmbedding(query).block();
            if (embedding == null || embedding.isEmpty()) {
                log.warn("Failed to generate embedding for query: {}", query);
                return Collections.emptyMap();
            }

            String queryVector = convertToVectorString(embedding);
            List<Object[]> results = embeddingRepository.findProductIdsBySimilarity(
                    queryVector, similarityThreshold);

            return results.stream()
                    .collect(Collectors.toMap(
                            row -> convertToUUID(row[0]),
                            row -> ((Number) row[1]).doubleValue(),
                            (v1, v2) -> v1 // Keep first value in case of duplicates
                    ));
        } catch (Exception e) {
            log.error("Error searching products by similarity for query: {}", query, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Check if embeddings exist for products
     */
    public long countEmbeddings() {
        return embeddingRepository.countEmbeddings();
    }

    @Transactional
    public void deleteProductEmbedding(UUID productId) {
        embeddingRepository.deleteByProductId(productId);
        log.info("Deleted embedding for product ID: {}", productId);
    }

    private String convertToVectorString(List<Double> embedding) {
        return "[" + String.join(",", embedding.stream()
                .map(String::valueOf)
                .toArray(String[]::new)) + "]";
    }

    private ProductEmbedding mapToProductEmbedding(Object[] row) {
        ProductEmbedding embedding = new ProductEmbedding();
        embedding.setId(((Number) row[0]).longValue());
        embedding.setProductId(convertToUUID(row[1]));
        embedding.setContent((String) row[2]);
        embedding.setEmbedding((String) row[3]);
        
        // Row[4] contains the similarity score from the query
        if (row.length > 4 && row[4] != null) {
            embedding.setSimilarity(((Number) row[4]).doubleValue());
        }
        return embedding;
    }

    private UUID convertToUUID(Object value) {
        if (value instanceof byte[]) {
            // Handle binary UUID from database (PostgreSQL bytea)
            byte[] bytes = (byte[]) value;
            if (bytes.length == 16) {
                long msb = 0;
                long lsb = 0;
                for (int i = 0; i < 8; i++) {
                    msb = (msb << 8) | (bytes[i] & 0xff);
                }
                for (int i = 8; i < 16; i++) {
                    lsb = (lsb << 8) | (bytes[i] & 0xff);
                }
                return new UUID(msb, lsb);
            }
        } else if (value instanceof UUID) {
            return (UUID) value;
        } else if (value instanceof String) {
            return UUID.fromString((String) value);
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to UUID");
    }
}
