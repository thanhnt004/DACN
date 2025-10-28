package com.example.backend.model.payment;

import com.example.backend.model.order.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "payments_order_id_fkey"))
    private Order order;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
