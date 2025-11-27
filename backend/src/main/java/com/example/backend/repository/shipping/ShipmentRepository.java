package com.example.backend.repository.shipping;

import com.example.backend.model.order.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findByOrderId(UUID id);

    Optional<Shipment> findByTrackingNumber(String s);
}
