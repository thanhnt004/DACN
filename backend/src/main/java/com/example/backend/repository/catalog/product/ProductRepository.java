package com.example.backend.repository.catalog.product;

import com.example.backend.dto.response.catalog.product.ProductSummaryResponse;
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

    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    Optional<Product> findBySlug(String slug);
}
