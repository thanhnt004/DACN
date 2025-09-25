package com.example.backend.repository.product;

import com.example.backend.model.product.Color;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ColorRepository extends GenericRepository<Color>{
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
    @Query(value = "select exists(" +
            "select 1 from product_variants pv where color_id = :id)",nativeQuery = true)
    Boolean usedByProduct(UUID id);
}
