package com.example.backend.model.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "product_images")
public class ProductImage {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_images_product"))
    private Product product;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    private String alt;

    @Column(nullable = false)
    @Builder.Default
    private int position = 0;

    private String publicId;

    private UUID colorId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void setValue(ProductImage image) {
        this.setAlt(image.getAlt());
        this.setColorId(image.getColorId());
        this.setPosition(image.getPosition());
        this.setImageUrl(image.getImageUrl());
    }
}
