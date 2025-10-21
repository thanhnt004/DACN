package com.example.backend.repository.catalog.product;

import com.example.backend.model.product.ProductVariant;
import com.example.backend.model.product.VariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    int countByProductId(UUID productId);
    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.status = :status WHERE pv.id = :id and pv.version = :version")
    int updateStatus(@Param("id") UUID id, @Param("status") VariantStatus status,@Param("version") int version);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByColor_IdAndSize_Id(UUID colorId, UUID sizeId);
}
