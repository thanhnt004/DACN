package com.example.backend.model.discount;

import com.example.backend.model.product.Category;
import com.example.backend.model.product.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "discounts", uniqueConstraints = @UniqueConstraint(name = "discounts_code_key", columnNames = {"code"}))
@Builder(toBuilder = true)
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
    @Enumerated(EnumType.STRING)
    private DiscountType type;

    @Column(name = "value", nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private Long value;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

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
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    //relations
    @ManyToMany(mappedBy = "discounts", fetch = FetchType.LAZY)
    private List<Product> products;
    @ManyToMany(mappedBy = "discounts", fetch = FetchType.LAZY)
    private List<Category> categories ;
    public enum DiscountType{
        PERCENTAGE,
        FIXED_AMOUNT
    }
}