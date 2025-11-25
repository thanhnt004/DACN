package com.example.backend.model.discount;

import com.example.backend.model.User;
import com.example.backend.model.discount.Discount;
import com.example.backend.model.order.Order;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "discount_redemptions", uniqueConstraints = @UniqueConstraint(name = "discount_redemptions_order_id_key", columnNames = {"order_id"}))
public class DiscountRedemption {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false, foreignKey = @ForeignKey(name = "discount_redemptions_discount_id_fkey"))
    private Discount discount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "discount_redemptions_user_id_fkey"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", foreignKey = @ForeignKey(name = "discount_redemptions_order_id_fkey"))
    private Order order;

    @Column(name = "redeemed_at", nullable = false)
    private Instant redeemedAt;
}