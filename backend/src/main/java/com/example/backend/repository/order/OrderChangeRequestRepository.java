package com.example.backend.repository.order;

import com.example.backend.model.order.OrderChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderChangeRequestRepository extends JpaRepository<OrderChangeRequest, UUID> {
    boolean existsByOrderIdAndStatus(UUID id, String pending);

    Optional<OrderChangeRequest> findByOrderId(UUID id);

    boolean existsByOrderIdAndStatusIn(UUID orderId, List<String> activeStatuses);
}
