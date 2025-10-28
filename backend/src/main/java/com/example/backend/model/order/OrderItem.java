package com.example.backend.model.order;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "sku")
    private String sku;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_amount", nullable = false)
    private Long unitPriceAmount;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount = 0L;

    @Column(name = "tax_amount", nullable = false)
    private Long taxAmount = 0L;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}