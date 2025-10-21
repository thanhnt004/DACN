package com.example.backend.repository.catalog.product;

import com.example.backend.dto.request.product.ProductView;
import com.example.backend.model.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);

    @Query("""
        select new com.example.backend.dto.response.product.ProductView(
            p.id,
            (select pi.imageUrl from ProductImage pi where pi.product.id = p.id order by pi.position asc limit 1),
            p.name,
            p.gender,
            p.priceAmount
        )
        from Product p
        where p.status = 'ACTIVE' and p.deletedAt = null
    """)
    List<ProductView> findAllProductViews();

    @Query(value = """
      SELECT p.id,
             (SELECT pi.image_url FROM product_images pi WHERE pi.product_id = p.id ORDER BY pi.position LIMIT 1) AS image_url,
             p.name,
             string_agg(DISTINCT c.hex_code, ',') AS colors_csv,
             p.gender,
             p.price
      FROM products p
      LEFT JOIN product_variants v ON v.product_id = p.id
      LEFT JOIN colors c ON c.id = v.color_id
      WHERE p.id IN (:ids)
      GROUP BY p.id, p.name, p.gender, p.price
      """, nativeQuery = true)
    List<Object[]> findProductViewsByIds(@Param("ids") List<UUID> ids);

    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Query("""
        select p.id, c.hexCode
        from ProductVariant v
        join v.product p
        join v.color c
        where p.id in :ids
    """)
    List<Object[]> findColorsByProductIds(@Param("ids") List<UUID> ids);
}
