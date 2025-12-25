package com.example.backend.repository.catalog.brand;

import com.example.backend.model.product.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID>, JpaSpecificationExecutor<Brand> {
    Boolean existsBySlugAndIdNot(String slug, UUID excludeId);

    Optional<Brand>  findBySlug(String slug);

    Optional<Brand> findByName(String normalizedName);
}
