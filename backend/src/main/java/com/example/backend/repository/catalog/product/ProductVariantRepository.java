package com.example.backend.repository.catalog.product;

import com.example.backend.dto.response.catalog.ColorDto;
import com.example.backend.dto.response.catalog.SizeDto;
import com.example.backend.model.product.ProductVariant;
import com.example.backend.model.product.VariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    int countByProductId(UUID productId);

    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.status = :status WHERE pv.id = :id and pv.version = :version")
    void updateStatus(@Param("id") UUID id, @Param("status") VariantStatus status, @Param("version") int version);

    boolean existsBySkuIgnoreCase(String sku);
    @Query("""
        SELECT DISTINCT new com.example.backend.dto.response.catalog.ColorDto(c.id, c.name, c.hexCode)
        FROM ProductVariant pv JOIN pv.color c
        WHERE pv.product.id = :productId
    """)
    List<ColorDto> findDistinctColorsByProductId(@Param("productId") UUID productId);
    @Query("""
    SELECT pv.product.id as productId, new com.example.backend.dto.response.catalog.ColorDto(c.id, c.name, c.hexCode)
    FROM ProductVariant pv JOIN pv.color c
    WHERE pv.product.id IN :productIds
""")
    List<Object[]> findColorsByProductIds(@Param("productIds") List<UUID> productIds);
    @Query("""
    SELECT pv.product.id as productId, new com.example.backend.dto.response.catalog.SizeDto(s.id, s.name, s.code)
    FROM ProductVariant pv JOIN pv.size s
    WHERE pv.product.id IN :productIds
""")
    List<Object[]> findSizesByProductIds(@Param("productIds") List<UUID> productIds);
    @Query("""
     select new com.example.backend.dto.response.catalog.SizeDto(
            s.id,
            s.code,
            s.name
        )
        from ProductVariant v
        join v.product p
        join v.size s
        where p.id = :productId
        group by s.id, s.code, s.name
""")
    List<SizeDto> getSizesByProductId(@Param("productId") UUID productId);

    @Query("SELECT v.priceAmount FROM ProductVariant v WHERE v.id = :variantId")
    Optional<Integer> getPriceById(UUID variantId);

    boolean existsByColor_IdAndSize_IdAndProductId(UUID colorId, UUID sizeId,UUID productId);
}
