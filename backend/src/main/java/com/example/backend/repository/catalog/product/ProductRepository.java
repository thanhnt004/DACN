package com.example.backend.repository.catalog.product;

import com.example.backend.dto.response.catalog.product.ProductSummaryResponse;
import com.example.backend.model.product.Category;
import com.example.backend.model.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);

    boolean existsBySlugIgnoreCase(String slug);

    @Query(value = "SELECT count(*) > 0 FROM products WHERE lower(slug) = lower(:slug)", nativeQuery = true)
    boolean existsBySlugIncludeDeleted(@Param("slug") String slug);

    @Query(value = "SELECT count(*) > 0 FROM products WHERE lower(slug) = lower(:slug) AND id != :id", nativeQuery = true)
    boolean existsBySlugAndIdNotIncludeDeleted(@Param("slug") String slug, @Param("id") UUID id);

    Optional<Product> findBySlug(String slug);

    @Query("SELECT DISTINCT c FROM Product p JOIN p.categories c WHERE p.id IN :productIds")
    List<Category> findDistinctCategoriesByProductIds(@Param("productIds") List<UUID> productIds);
}
