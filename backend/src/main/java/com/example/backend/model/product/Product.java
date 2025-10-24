package com.example.backend.model.product;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.awt.*;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@SQLDelete(sql = "UPDATE products SET deleted_at = now() WHERE id = ?")
@SQLRestriction(value = "deleted_at IS NULL")
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false,unique = true)
    private String slug;

    private String description;

    private String material;

    @Builder.Default
    @Enumerated(value = EnumType.STRING)
    private Gender gender = Gender.unisex;

    @Builder.Default
    @Enumerated(value = EnumType.STRING)
    private ProductStatus status = ProductStatus.DRAFT;

    private String seoTitle;

    @Column(name = "price", nullable = false)
    private long priceAmount;

    private String seoDescription;

    private int soldCount;

    private int ratingCount;

    private int ratingSum;

    @Column(columnDefinition = "NUMERIC(3,2)",updatable = false,insertable = false)
    private double ratingAvg;

    @Builder.Default
    private Boolean isInStock = true;

    private String primaryImageUrl;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Version
    private Integer version;

    //relations
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories ;


    private UUID brandId;

    // Images
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, createdAt ASC")
    private List<ProductImage> images = new ArrayList<>();

    // Variants
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    public void addImage(ProductImage img) {
        boolean hasDefaultImage = false;
        for (ProductImage productImage:images)
        {
            if (productImage.isDefault())
            {
                hasDefaultImage = true;
                if (img.isDefault())
                {
                    this.primaryImageUrl = img.getImageUrl();
                    productImage.setDefault(false);
                }
                break;
            }
        }
        if (!hasDefaultImage)
        {
            img.setDefault(true);
            this.primaryImageUrl = img.getImageUrl();
        }
        images.add(img);
        img.setProduct(this);
    }
    public void removeImage(ProductImage img) {
        if (img.isDefault())

        images.remove(img);
        img.setProduct(null);
    }

    public void syncImage(List<ProductImage> images) {
        if (images == null)
            return;
        Map<UUID,ProductImage> existedById = getImages().stream()
                .collect(Collectors.toMap(ProductImage::getId, img->img));
        Set<ProductImage> toRemove = new HashSet<>(this.getImages());
        for (ProductImage image:images)
        {
            if (image.getId() != null)
            {
                ProductImage existing = existedById.get(image.getId());
                if (existing == null)
                    throw new IllegalArgumentException("Cannot resolve");
                existing.setValue(image);
                toRemove.remove(existing);
            }else
            {
                this.addImage(image);
            }
        }
        for (int i = 0;i< images.size();i++)
        {
            images.get(i).setPosition(i);
        }
        toRemove.forEach(this::removeImage);
    }
    //variant
    public void addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
    }
    public void removeVariant(ProductVariant variant) {
        variants.remove(variant);
        variant.setProduct(null);
    }

}
