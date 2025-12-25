package com.example.backend.model.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
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
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_images_product"))
    private Product product;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", foreignKey = @ForeignKey(name = "fk_product_images_variant"))
    private ProductVariant variant;
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    private String alt;

    @Column(nullable = false)
    private int position;

    private String publicId;

    private boolean isDefault ;

    private UUID colorId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public void setValue(ProductImage image) {
        this.setAlt(image.getAlt());
        this.setColorId(image.getColorId());
        this.setPosition(image.getPosition());
        this.setImageUrl(image.getImageUrl());
        this.setPublicId(image.getPublicId());
        this.setDefault(image.isDefault());
        this.setCreatedAt(image.getCreatedAt());
        this.setVariant(image.getVariant());
    }
}
