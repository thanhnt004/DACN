package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_embeddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    private UUID productId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding", columnDefinition = "vector(768)")
    private String embedding;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "embedding_version", nullable = false)
    private Integer embeddingVersion = 1;

    @Transient
    private Double similarity; // Similarity score from vector search (not stored in DB)
}

