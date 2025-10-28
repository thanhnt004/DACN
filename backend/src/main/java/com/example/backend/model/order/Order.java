package com.example.backend.model.order;

import com.example.backend.model.User;
import com.example.backend.model.payment.Payment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

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

    @Column(name = "tax_amount", nullable = false)
    private Long taxAmount = 0L;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount = 0L;

    // store jsonb as String for now; use custom type if you want to map to Map/POJO
    @Column(name = "shipping_address", columnDefinition = "jsonb")
    private String shippingAddress;

    @Column(name = "notes")
    private String notes;

    @Column(name = "placed_at")
    private LocalDateTime placedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    public enum OrderStatus{
        PENDING,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        REFUNDED
    }
    public void addItem(OrderItem item)
    {
        this.items.add(item);
        item.setOrder(this);
    }
    public Payment getActivePayment()
    {
        return this.getPayments().stream().filter(pm->pm.getStatus().equals(Payment.PaymentStatus.PENDING)).toList().getFirst();
    }
}