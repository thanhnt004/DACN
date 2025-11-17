package com.example.backend.service.product;

import com.example.backend.dto.response.catalog.VariantStockStatus;
import com.example.backend.excepton.ConflictException;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.model.order.Order;
import com.example.backend.model.product.Inventory;
import com.example.backend.model.product.InventoryReservation;
import com.example.backend.repository.catalog.product.InventoryRepository;
import com.example.backend.repository.catalog.product.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;

    /**
     * Lấy số lượng tồn kho khả dụng
     */
    @Transactional(readOnly = true)
    public int getAvailableStock(UUID variantId) {
        Inventory inventory = inventoryRepository.findById(variantId)
                .orElse(Inventory.builder()
                        .id(variantId)
                        .quantityOnHand(0)
                        .quantityReserved(0)
                        .build());

        return inventory.getQuantityOnHand() - inventory.getQuantityReserved();
    }

    /**
     * Đặt giữ hàng tồn kho cho order
     */
    @Transactional
    public void reserveStockForOrder(Order order) {
        log.info("Reserving stock for order: {}", order.getOrderNumber());

        order.getItems().forEach(orderItem -> {
            UUID variantId = orderItem.getVariant().getId();
            int quantity = orderItem.getQuantity();

            // Lock inventory row for update
            Inventory inventory = inventoryRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new NotFoundException(
                            "Variant không tồn tại: " + variantId
                    ));

            // Check available stock
            int available = inventory.getQuantityOnHand() - inventory.getQuantityReserved();
            if (available < quantity) {
                throw new ConflictException(
                        String.format(
                                "Không đủ hàng: %s. Còn %d, yêu cầu %d",
                                orderItem.getProductName(),
                                available,
                                quantity
                        )
                );
            }

            // Reserve stock
            inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
            inventoryRepository.save(inventory);

            // Create reservation record
            InventoryReservation reservation = InventoryReservation.builder()
                    .reservedAt(Instant.now())
                    .order(order)
                    .variant(orderItem.getVariant())
                    .quantity(quantity)
                    .build();

            reservationRepository.save(reservation);

            log.debug("Reserved {} units of variant {} for order {}",
                    quantity, variantId, order.getId());
        });

        log.info("Stock reserved successfully for order: {}", order.getOrderNumber());
    }

    /**
     * Giải phóng reservation (khi cancel order)
     */
    @Transactional
    public void releaseReservation(Order order) {
        log.info("Releasing stock reservation for order: {}", order.getOrderNumber());

        order.getItems().forEach(orderItem -> {
            UUID variantId = orderItem.getVariant().getId();
            int quantity = orderItem.getQuantity();

            Inventory inventory = inventoryRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new RuntimeException(
                            "Variant không tồn tại: " + variantId
                    ));

            inventory.setQuantityReserved(
                    Math.max(0, inventory.getQuantityReserved() - quantity)
            );
            inventoryRepository.save(inventory);

            // Update reservation record
            reservationRepository.findByOrderIdAndVariantId(order.getId(), variantId)
                    .ifPresent(reservation -> {
                        reservation.setReleasedAt(Instant.now());
                        reservationRepository.save(reservation);
                    });
        });

        log.info("Stock reservation released for order: {}", order.getOrderNumber());
    }

    /**
     * Confirm sold (chuyển từ reserved → sold khi order delivered)
     */
    @Transactional
    public void confirmSold(Order order) {
        log.info("Confirming sold for order: {}", order.getOrderNumber());

        order.getItems().forEach(orderItem -> {
            UUID variantId = orderItem.getVariant().getId();
            int quantity = orderItem.getQuantity();

            Inventory inventory = inventoryRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new RuntimeException(
                            "Variant không tồn tại: " + variantId
                    ));

            // Giảm cả on_hand và reserved
            inventory.setQuantityOnHand(
                    Math.max(0, inventory.getQuantityOnHand() - quantity)
            );
            inventory.setQuantityReserved(
                    Math.max(0, inventory.getQuantityReserved() - quantity)
            );

            inventoryRepository.save(inventory);
        });

        log.info("Sold confirmed for order: {}", order.getOrderNumber());
    }

    public Map<UUID, VariantStockStatus> getStockStatusForVariants(List<UUID> ids) {
        List<Inventory> inventories = inventoryRepository.findAllById(ids);
        return inventories.stream().collect(
                Collectors.toMap(
                        Inventory::getId,
                        inv -> VariantStockStatus.builder()
                                .inStock(inv.isInStock())
                                .availableQuantity(
                                        inv.getQuantityOnHand() - inv.getQuantityReserved()
                                )
                                .message(inv.getAvailableStock()<=10?"Sắp hết hàng":"")
                                .build(
                )
        ));
    }
}
