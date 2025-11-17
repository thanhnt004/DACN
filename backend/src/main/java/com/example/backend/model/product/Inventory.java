package com.example.backend.model.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "inventory")
public class Inventory {

    @Id
    @Column(name = "variant_id", columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "variant_id", foreignKey = @ForeignKey(name = "fk_inventory_variant"))
    private ProductVariant variant;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand ;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved ;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public int getAvailableStock() {
        return quantityOnHand - quantityReserved;
    }
    public boolean isInStock() {
        return getAvailableStock() >= 0;
    }
    public boolean isBelowReorderLevel() {
        return getAvailableStock() <= reorderLevel;
    }
}
