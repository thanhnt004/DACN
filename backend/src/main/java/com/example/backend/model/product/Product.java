package com.example.backend.model.product;

import com.example.backend.exception.product.ProductImageNotFoundException;
import com.example.backend.model.discount.Discount;
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
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@SQLDelete(sql = "UPDATE products SET deleted_at = now() WHERE id = ? and version = ?")
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

    @Column(name = "price")
    private long priceAmount;

    private String seoDescription;

    private int soldCount;

    @Builder.Default
    private Boolean isInStock = true;

    private String primaryImageUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant deletedAt;

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
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "discount_products",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "discount_id")
    )
    private List<Discount> discounts ;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id",
            foreignKey = @ForeignKey(name = "products_brand_id_fkey"))
    private Brand brand;
    // Images
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, createdAt ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    // Variants
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
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
        if (img.isDefault()) {
            this.primaryImageUrl = null;
            if (images.size() > 1) {
                for (ProductImage productImage : images) {
                    if (productImage.getId()==null||!productImage.getId().equals(img.getId())) {
                        productImage.setDefault(true);
                        this.primaryImageUrl = productImage.getImageUrl();
                        break;
                    }
                }
            }
        }
        images.remove(img);
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
                    throw new ProductImageNotFoundException("Không thể tìm thấy ảnh sản phẩm với ID: " + image.getId());
                existing.setValue(image);
                existing.setDefault(image.isDefault());
                if (existing.isDefault()) {
                    this.primaryImageUrl = existing.getImageUrl();
                }
                toRemove.remove(existing);
            }else
            {
                this.addImage(image);
            }
        }
        toRemove.forEach(this::removeImage);

        for (int i = 0;i< images.size();i++)
        {
            images.get(i).setPosition(i);
        }
        boolean hasDefault = this.images.stream().anyMatch(ProductImage::isDefault);
        if (!hasDefault && !this.images.isEmpty()) {
            // Tự động chọn ảnh đầu tiên làm default nếu chưa có
            ProductImage firstImg = this.images.get(0);
            firstImg.setDefault(true);
            this.primaryImageUrl = firstImg.getImageUrl();
        } else if (this.images.isEmpty()) {
            this.primaryImageUrl = null;
        }
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
