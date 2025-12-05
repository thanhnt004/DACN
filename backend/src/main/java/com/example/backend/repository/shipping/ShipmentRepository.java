package com.example.backend.repository.shipping;

import com.example.backend.model.order.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByTrackingNumber(String s);

    List<Shipment> findAllByOrderId(UUID id);

    Optional<Shipment> findByOrderIdAndIsActiveTrue(UUID orderId);

    @Query(
        "SELECT s.trackingNumber FROM Shipment s WHERE s.isActive = true AND s.order.id IN :orderIds"
    )
    List<String> getAllTrackingNumbersForOrders(List<UUID> orderIds);
}
