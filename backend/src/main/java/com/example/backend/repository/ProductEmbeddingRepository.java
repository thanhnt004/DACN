package com.example.backend.repository;


import com.example.backend.entity.ProductEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductEmbeddingRepository extends JpaRepository<ProductEmbedding, Long> {
    Optional<ProductEmbedding> findByProductId(UUID productId);

    void deleteByProductId(UUID productId);

    @Query(value = """
        SELECT pe.*, 1 - (pe.embedding <=> CAST(:queryVector AS vector)) as similarity
        FROM product_embeddings pe
        WHERE 1 - (pe.embedding <=> CAST(:queryVector AS vector)) > :threshold
        ORDER BY pe.embedding <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarProducts(
        @Param("queryVector") String queryVector,
        @Param("threshold") Double threshold,
        @Param("limit") Integer limit
    );
    
    @Query(value = """
        SELECT pe.product_id, 1 - (pe.embedding <=> CAST(:queryVector AS vector)) as similarity
        FROM product_embeddings pe
        WHERE 1 - (pe.embedding <=> CAST(:queryVector AS vector)) > :threshold
        ORDER BY pe.embedding <=> CAST(:queryVector AS vector)
        """, nativeQuery = true)
    List<Object[]> findProductIdsBySimilarity(
        @Param("queryVector") String queryVector,
        @Param("threshold") Double threshold
    );
    
    @Query("SELECT pe.productId FROM ProductEmbedding pe")
    List<UUID> findAllProductIds();

    @Query("SELECT COUNT(pe) FROM ProductEmbedding pe")
    long countEmbeddings();
}
