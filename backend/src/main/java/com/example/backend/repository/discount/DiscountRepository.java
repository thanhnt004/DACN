package com.example.backend.repository.discount;

import com.example.backend.model.discount.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discount, UUID>, JpaSpecificationExecutor<Discount> {
    Optional<Discount> findByCode(String code);

    boolean existsByCode(String code);
    @Query("SELECT DISTINCT d FROM Discount d " +
            "LEFT JOIN d.products p " +
            "LEFT JOIN d.categories c " +
            "WHERE d.active = true " +
            "AND (d.startsAt IS NULL OR d.startsAt <= :now) " +
            "AND (d.endsAt IS NULL OR d.endsAt >= :now) " +
            "AND ( " +
            "   p.id IN :productIds " +
            "   OR c.id IN :categoryIds " +
            "   OR (p IS NULL AND c IS NULL) " + // Include site-wide discounts
            ")")
    List<Discount> findApplicableAndGlobalDiscounts(
            @Param("productIds") List<UUID> productIds,
            @Param("categoryIds") List<UUID> categoryIds,
            @Param("now") LocalDateTime now
    );
}
