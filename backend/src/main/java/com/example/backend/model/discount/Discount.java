package com.example.backend.model.discount;

import com.example.backend.model.product.Category;
import com.example.backend.model.product.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "discounts", uniqueConstraints = @UniqueConstraint(name = "discounts_code_key", columnNames = {"code"}))
public class Discount {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "value", nullable = false)
    private Integer value;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "min_order_amount")
    private Long minOrderAmount;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    //relations
    @ManyToMany(mappedBy = "discounts", fetch = FetchType.LAZY)
    private List<Product> products;
    @ManyToMany(mappedBy = "discounts", fetch = FetchType.LAZY)
    private List<Category> categories ;
}