package com.example.backend.repository.product;

import com.example.backend.model.product.Size;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface SizeRepository extends GenericRepository<Size> {
    boolean existsByCodeAndIdNot(String code, UUID id);
    @Query(value = "select exists(" +
            "select 1 from product_variants pv where size_id = :id)",nativeQuery = true)
    Boolean usedByProduct(UUID id);
}
