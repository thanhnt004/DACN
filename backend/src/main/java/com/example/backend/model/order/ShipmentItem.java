package com.example.backend.model.order;

import com.example.backend.model.order.OrderItem;
import com.example.backend.model.order.Shipment;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "shipment_items")
public class ShipmentItem {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, foreignKey = @ForeignKey(name = "shipment_items_shipment_id_fkey"))
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, foreignKey = @ForeignKey(name = "shipment_items_order_item_id_fkey"))
    private OrderItem orderItem;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}