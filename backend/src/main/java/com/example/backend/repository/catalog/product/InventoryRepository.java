package com.example.backend.repository.catalog.product;

import com.example.backend.model.product.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    @Modifying
    @Query(value = "UPDATE inventory SET quantity_reserved = quantity_reserved + :qty, updated_at = now() " +
            "WHERE variant_id = :variantId AND (quantity_on_hand - quantity_reserved) >= :qty",
            nativeQuery = true)
    int reserveIfAvailable(@Param("variantId") UUID variantId, @Param("qty") int qty);
    /**
     * Cập nhật số lượng 'reserved' một cách an toàn (atomic).
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantityReserved = i.quantityReserved + :quantity " +
            "WHERE i.id =:variantId")
    void reserveStock(@Param("variantId") UUID variantId, @Param("quantity") int quantity);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :variantId")
    Optional<Inventory> findByIdForUpdate(UUID variantId);
}
