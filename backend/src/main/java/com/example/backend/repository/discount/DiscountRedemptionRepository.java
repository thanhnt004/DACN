package com.example.backend.repository.discount;


import com.example.backend.model.discount.DiscountRedemption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscountRedemptionRepository extends JpaRepository<DiscountRedemption, UUID> {
    Page<DiscountRedemption> findByDiscountId(UUID id, Pageable pageable);

    Long countByDiscountId(UUID id);

    long countByDiscountIdAndUserId(UUID id, UUID userId);
}
