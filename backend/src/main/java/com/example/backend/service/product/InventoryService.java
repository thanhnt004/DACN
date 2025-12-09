package com.example.backend.service.product;

import com.example.backend.dto.response.catalog.VariantStockStatus;
import com.example.backend.exception.NotFoundException;
import com.example.backend.exception.cart.InsufficientStockException;
import com.example.backend.model.order.Order;
import com.example.backend.model.order.OrderItem;
import com.example.backend.model.product.Inventory;
import com.example.backend.model.product.InventoryReservation;
import com.example.backend.repository.catalog.product.InventoryRepository;
import com.example.backend.repository.catalog.product.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Transactional
    public void reserveStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            // Safety check for reservation too
            if (item.getVariant() == null) {
                throw new InsufficientStockException("Product data is corrupted (Variant not found) for item: " + item.getProductName());
            }

            UUID variantId = item.getVariant().getId();
            int qty = item.getQuantity();

            int updated = inventoryRepository.reserveIfAvailable(variantId, qty);
            if (updated == 0) {
                throw new InsufficientStockException("Không đủ hàng: " + item.getProductName());
            }

            InventoryReservation r = InventoryReservation.builder()
                    .variant(item.getVariant())
                    .order(order)
                    .quantity(qty)
                    .reservedAt(Instant.now())
                    .build();
            reservationRepository.save(r);
        }
        log.info("Stock reserved successfully for order: {}", order.getOrderNumber());
    }

    /**
     * FIXED: Safe release logic
     */
    @Transactional
    public void releaseReservation(Order order) {
        log.info("Releasing stock reservation for order: {}", order.getOrderNumber());

        order.getItems().forEach(orderItem -> {
            // 1. CHECK NULL FIRST
            if (orderItem.getVariant() == null) {
                log.warn("Cannot release stock for OrderItem {}. Variant likely deleted physically.", orderItem.getId());
                return; // Skip this iteration, do not crash
            }

            // 2. NOW it is safe to access ID
            UUID variantId = orderItem.getVariant().getId();
            int quantity = orderItem.getQuantity();

            Inventory inventory = inventoryRepository.findByIdForUpdate(variantId)
                    .orElse(null);

            if (inventory == null) {
                log.warn("Inventory record not found for variant {}. Skipping stock release.", variantId);
            } else {
                inventory.setQuantityReserved(Math.max(0, inventory.getQuantityReserved() - quantity));
                inventoryRepository.save(inventory);
            }

            // Update reservation record
            reservationRepository.findByOrderIdAndVariantId(order.getId(), variantId)
                    .ifPresent(reservation -> {
                        reservation.setReleasedAt(Instant.now());
                        reservationRepository.save(reservation);
                    });
        });

        log.info("Stock reservation released for order: {}", order.getOrderNumber());
    }

    @Transactional
    public void confirmSold(Order order) {
        log.info("Confirming sold for order: {}", order.getOrderNumber());

        order.getItems().forEach(orderItem -> {
            if (orderItem.getVariant() == null) {
                log.warn("Skipping confirmSold for item {}: Variant deleted.", orderItem.getId());
                return;
            }

            UUID variantId = orderItem.getVariant().getId();
            int quantity = orderItem.getQuantity();

            Inventory inventory = inventoryRepository.findByIdForUpdate(variantId)
                    .orElse(null);

            if (inventory == null) {
                log.warn("Inventory record not found for variant {}. Skipping confirm sold.", variantId);
            } else {
                inventory.setQuantityOnHand(Math.max(0, inventory.getQuantityOnHand() - quantity));
                inventory.setQuantityReserved(Math.max(0, inventory.getQuantityReserved() - quantity));

                inventoryRepository.save(inventory);
            }
        });

        log.info("Sold confirmed for order: {}", order.getOrderNumber());
    }

    @Transactional
    public void revertSold(Order order) {
        log.info("Reverting sold for order: {}", order.getOrderNumber());

        order.getItems().forEach(orderItem -> {
            if (orderItem.getVariant() == null) {
                log.warn("Skipping revertSold for item {}: Variant deleted.", orderItem.getId());
                return;
            }

            UUID variantId = orderItem.getVariant().getId();
            int quantity = orderItem.getQuantity();

            Inventory inventory = inventoryRepository.findByIdForUpdate(variantId)
                    .orElse(null);

            if (inventory == null) {
                log.warn("Inventory record not found for variant {}. Skipping revert sold.", variantId);
            } else {
                inventory.setQuantityOnHand(inventory.getQuantityOnHand() + quantity);
                inventoryRepository.save(inventory);
            }
        });

        log.info("Sold reverted for order: {}", order.getOrderNumber());
    }

    public Map<UUID, VariantStockStatus> getStockStatusForVariants(List<UUID> ids) {
        List<Inventory> inventories = inventoryRepository.findAllById(ids);
        return inventories.stream().collect(
                Collectors.toMap(
                        Inventory::getId,
                        inv -> VariantStockStatus.builder()
                                .inStock(inv.isInStock())
                                .availableQuantity(inv.getQuantityOnHand() - inv.getQuantityReserved())
                                .message((inv.getQuantityOnHand() - inv.getQuantityReserved()) <= 10 ? "Sắp hết hàng" : "")
                                .build()
                )
        );
    }
}