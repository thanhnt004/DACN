package com.example.backend.repository.order;

import com.example.backend.model.User;
import com.example.backend.model.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    boolean existsByOrderNumber(String orderNumber);

    Optional<Order> findByOrderNumber(String orderNumber);

}
