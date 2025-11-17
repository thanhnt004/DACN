package com.example.backend.repository.catalog.product;

import com.example.backend.model.product.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    Optional<InventoryReservation> findByOrderIdAndVariantId(UUID id, UUID variantId);
}
