package com.example.backend.repository.discount;

import com.example.backend.model.DiscountRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiscountRedemptionRepository extends JpaRepository<DiscountRedemption, UUID> {
}
