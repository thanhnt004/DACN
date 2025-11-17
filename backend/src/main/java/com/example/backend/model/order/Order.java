package com.example.backend.model.order;

import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.model.DiscountRedemption;
import com.example.backend.model.User;
import com.example.backend.model.payment.Payment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", uniqueConstraints = {
        @UniqueConstraint(name = "orders_order_number_key", columnNames = {"order_number"})
})
@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
    private UUID guestId;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "subtotal_amount", nullable = false)
    private Long subtotalAmount = 0L;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount = 0L;

    @Column(name = "shipping_amount", nullable = false)
    private Long shippingAmount = 0L;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount = 0L;

    @Column(name = "shipping_address", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private UserAddress shippingAddress;

    @Column(name = "notes")
    private String notes;

    @Column(name = "placed_at")
    private Instant placedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "paid_at")
    private Instant paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    @OneToMany(mappedBy = "order",cascade = CascadeType.PERSIST)
    private List<DiscountRedemption> discountRedemptions;

    public void addPayment(Payment payment) {
        if (this.getPayments() == null) {
            this.payments = new ArrayList<>();
        }
        this.payments.add(payment);
        payment.setOrder(this);
    }

    public boolean isPaid() {
        return this.getStatus() == OrderStatus.CONFIRMED;
    }

    public enum OrderStatus{
        PENDING,
        CONFIRMED,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        REFUNDED
    }
    public void addItem(OrderItem item)
    {
        this.items = new ArrayList<>();
        this.items.add(item);
        item.setOrder(this);
    }
    public Payment getActivePayment()
    {
        return this.getPayments().stream().filter(pm->pm.getStatus().equals(Payment.PaymentStatus.PENDING)).toList().getFirst();
    }
}