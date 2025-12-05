package com.example.backend.repository.order;

import com.example.backend.model.User;
import com.example.backend.model.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    boolean existsByOrderNumber(String orderNumber);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o JOIN o.payments p WHERE o.status = com.example.backend.model.order.Order.OrderStatus.PENDING AND o.paidAt IS NULL AND upper(p.provider) NOT LIKE '%COD%' AND o.createdAt < :thirtyMinutesAgo")
    List<Order> findExpiredPendingOrders(@Param("thirtyMinutesAgo") Instant thirtyMinutesAgo);
}
