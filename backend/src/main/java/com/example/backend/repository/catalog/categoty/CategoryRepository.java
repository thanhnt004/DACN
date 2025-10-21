package com.example.backend.repository.catalog.categoty;

import com.example.backend.model.product.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    boolean existsBySlugAndIdNot(String slug, UUID id);


    @Query("select c from Category c where (:includeDeleted = true or c.deletedAt is null) order by c.name desc")
    List<Category> findRootCategory(@Param("includeDeleted") Boolean includeDeleted);
    // lấy children nhanh
    @Query("select c from Category c where (:includeDeleted = true or c.deletedAt is null) and c.id = :parentId order by c.name desc"
    )
    List<Category> findChildren(@Param("parentId")UUID parentId,
                                @Param("includeDeleted") Boolean includeDeleted);

    @Query("SELECT COUNT(p) FROM Product p JOIN p.categories c WHERE c.id = :categoryId")
    long countProduct(@Param("categoryId") UUID categoryId);

    boolean existsByParentId(UUID categoryId);

}