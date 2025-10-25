package com.example.backend.repository.catalog.product;

import com.example.backend.dto.response.catalog.ColorDto;
import com.example.backend.dto.response.catalog.SizeDto;
import com.example.backend.model.product.ProductVariant;
import com.example.backend.model.product.VariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    int countByProductId(UUID productId);

    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.status = :status WHERE pv.id = :id and pv.version = :version")
    void updateStatus(@Param("id") UUID id, @Param("status") VariantStatus status, @Param("version") int version);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByColor_IdAndSize_Id(UUID colorId, UUID sizeId);

    @Query("""
        select new com.example.backend.dto.response.catalog.ColorDto(
            c.id,
            c.name,
            c.hexCode
        )
        from ProductVariant v
        join v.product p
        join v.color c
        where p.id in :productIds
        group by c.hexCode
         """)
    List<ColorDto> getColorsByProductId(UUID productId);
    @Query("""
    
""")
    List<SizeDto> getSizesByProductId(UUID productId);
}
