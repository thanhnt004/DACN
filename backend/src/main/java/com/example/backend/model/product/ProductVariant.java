package com.example.backend.model.product;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_variants_sku", columnNames = "sku")
        },
        indexes = {
                @Index(name = "ix_product_variants_product", columnList = "product_id"),
                @Index(name = "ix_product_variants_size_color", columnList = "product_id,size_id,color_id")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE product_variants SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@DynamicUpdate
public class ProductVariant {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_variants_product"))
    private Product product;

    @Column(nullable = false)
    private String sku;

    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_id", foreignKey = @ForeignKey(name = "fk_product_variants_size"))
    private Size size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id", foreignKey = @ForeignKey(name = "fk_product_variants_color"))
    private Color color;

    @Column(name = "price_amount", nullable = false)
    private long priceAmount;

    @Column(name = "compare_at_amount")
    private Long compareAtAmount;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VariantStatus status = VariantStatus.ACTIVE;

    @UpdateTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreationTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    // One-to-one inventory
    @OneToOne(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private Inventory inventory;
    public void setInventory(Inventory inventory)
    {
        this.inventory = inventory;
        inventory.setVariant(this);
    }
}
