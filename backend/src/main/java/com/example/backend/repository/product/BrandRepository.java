package com.example.backend.repository.product;

import com.example.backend.model.product.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
    Boolean existsBySlugAndIdNot(String slug, UUID excludeId);
}
