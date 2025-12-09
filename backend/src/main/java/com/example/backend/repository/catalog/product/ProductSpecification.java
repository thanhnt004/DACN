package com.example.backend.repository.catalog.product;

import com.example.backend.model.product.*;
import com.example.backend.entity.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ProductSpecification {
    public static Specification<Product> hasStatus(ProductStatus status){
        return (root, query, builder) -> status == null ? null : builder.equal(root.get(Product_.status), status);
    }

    /**
     * Filter products by IDs (used for vector search results)
     */
    public static Specification<Product> hasIdIn(Set<UUID> productIds) {
        return (root, query, builder) -> {
            if (productIds == null || productIds.isEmpty()) {
                return null;
            }
            return root.get(Product_.id).in(productIds);
        };
    }
    public static Specification<Product> hasMinPrice(Long minPriceAmount) {
        return (root, query, builder) -> minPriceAmount == null? null
                : builder.greaterThanOrEqualTo(root.get(Product_.priceAmount), minPriceAmount);
    }

    public static Specification<Product> hasMaxPrice(Long maxPriceAmount) {
        return (root, query, builder) -> maxPriceAmount == null? null
                : builder.lessThanOrEqualTo(root.get(Product_.priceAmount), maxPriceAmount);
    }

    public static Specification<Product> hasBrand(UUID brandId) {
        return (root, query, builder) -> {
            if (brandId == null) {
                return null;
            }
            return builder.equal(root.get(Product_.brand).get(Brand_.id), brandId);
        };
    }

    // Lọc theo UUID trực tiếp trên Product
    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, builder) ->{
            if (categoryId == null)
                return null;
            Join<Product, Category> categoryJoin = root.join("categories");
            return builder.equal(categoryJoin.get("id"), categoryId);
        };
    }
    public static Specification<Product> hasVariantSizes(List<UUID> sizeIds) {
        return (root, query, builder) -> {
            // Kiểm tra điều kiện đầu vào
            if (sizeIds == null || sizeIds.isEmpty()) {
                return null;
            }

            // 1. Tạo Subquery dựa trên ProductVariant
            Subquery<ProductVariant> subquery = query.subquery(ProductVariant.class);
            Root<ProductVariant> subRoot = subquery.from(ProductVariant.class);

            // 2. Tương quan (Correlate) Subquery với Product gốc
            Predicate correlation = builder.equal(
                    root.get(Product_.id),
                    subRoot.get(ProductVariant_.product).get(Product_.id)
            );

            // 3. Áp dụng điều kiện lọc (Size ID IN List)
            Expression<UUID> sizeIdPath = subRoot.get(ProductVariant_.size).get(Size_.id);
            Predicate sizeFilter = sizeIdPath.in(sizeIds);

            subquery.select(subRoot).where(builder.and(correlation, sizeFilter));
            return builder.exists(subquery);
        };
    }

    /**
     * Tìm kiếm Product mà có ÍT NHẤT MỘT ProductVariant có Color ID nằm trong colorIds.
     */
    public static Specification<Product> hasVariantColors(List<UUID> colorIds) {
        return (root, query, builder) -> {
            if (colorIds == null || colorIds.isEmpty()) {
                return null;
            }

            Subquery<ProductVariant> subquery = query.subquery(ProductVariant.class);
            Root<ProductVariant> subRoot = subquery.from(ProductVariant.class);

            Predicate correlation = builder.equal(
                    root.get(Product_.id),
                    subRoot.get(ProductVariant_.product).get(Product_.id)
            );

            // Dùng path traversal: subRoot -> color Entity -> id
            Expression<UUID> colorIdPath = subRoot.get(ProductVariant_.color).get(Color_.id);
            Predicate colorFilter = colorIdPath.in(colorIds);

            subquery.select(subRoot).where(builder.and(correlation, colorFilter));
            return builder.exists(subquery);
        };
    }

    /**
     * Fetch Brand association to avoid LazyInitializationException
     */
    public static Specification<Product> fetchBrand() {
        return (root, query, builder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch(Product_.brand, JoinType.LEFT);
            }
            return null;
        };
    }
}
