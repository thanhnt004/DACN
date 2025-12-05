package com.example.backend.service.shipping;

import com.example.backend.dto.ghn.GhnWebhookRequest;
import com.example.backend.exception.shipping.ShippingNotFoundException;
import com.example.backend.model.order.Order;
import com.example.backend.model.order.Shipment;
import com.example.backend.repository.order.OrderRepository;
import com.example.backend.repository.shipping.ShipmentRepository;
import com.example.backend.service.product.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class ShipmentSyncService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;

    // Mapping trạng thái GHN -> System Shipment Status
    private static final Map<String, String> STATUS_MAPPING = Map.of(
            "ready_to_pick", "READY_TO_PICK",
            "picked", "SHIPPED",
            "storing", "IN_TRANSIT",
            "delivering", "DELIVERING",
            "delivered", "DELIVERED",
            "return", "RETURNING",
            "returned", "RETURNED",
            "cancel", "CANCELLED"
    );
    private final InventoryService inventoryService;

    public ShipmentSyncService(ShipmentRepository shipmentRepository, OrderRepository orderRepository, InventoryService inventoryService) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public void updateShipmentStatus(GhnWebhookRequest request) {
        // 1. Tìm Shipment theo Tracking Number (OrderCode của GHN)
        Shipment shipment = shipmentRepository.findByTrackingNumber(request.orderCode())
                .orElseThrow(() -> new ShippingNotFoundException("Shipment not found for code: " + request.orderCode()));

        // 2. Map trạng thái
        String newStatus = STATUS_MAPPING.getOrDefault(request.status(), "UNKNOWN");
        shipment.setStatus(newStatus);
        String newWarehouse = request.warehouse();
        if (newWarehouse != null && !newWarehouse.isEmpty()) {
            shipment.setWarehouse(newWarehouse);
        }
        // Cập nhật thời gian
        Instant eventTime = Instant.ofEpochMilli(request.time());
        if ("SHIPPED".equals(newStatus)) {
            shipment.setShippedAt(eventTime);
        } else if ("DELIVERED".equals(newStatus)) {
            shipment.setDeliveredAt(eventTime);
        }

        shipmentRepository.save(shipment);

        // 3. Cập nhật trạng thái Order chính (Đồng bộ Order Status)
        updateOrderStatus(shipment, newStatus, eventTime);
    }

    private void updateOrderStatus(Shipment shipment, String shipmentStatus, Instant eventTime) {
        Order order = shipment.getOrder();
        if (order == null) return;

        switch (shipmentStatus) {
            case "DELIVERED" -> {
                order.setStatus(Order.OrderStatus.DELIVERED);
                if (order.getPaidAt() == null)
                    order.setPaidAt(eventTime);
                if (shipment.isReturnShipment())
                {
                    order.setStatus(Order.OrderStatus.RETURNED);
                    inventoryService.revertSold(order);
                }
                else
                    inventoryService.confirmSold(order);
            }
            case "RETURNED" -> {
                order.setStatus(Order.OrderStatus.RETURNED);
                inventoryService.releaseReservation(order);
            }
            case "CANCELLED" -> order.setStatus(Order.OrderStatus.CANCELLED);
            case "DELIVERING" -> order.setStatus(Order.OrderStatus.SHIPPED);
        }
        orderRepository.save(order);
    }
}