package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Addresses")
public class Address {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false,length = 30)
    private String phone;
    @Column(nullable = false)
    private String line1;
    private String line2;
    private String ward;
    private String district;
    private String province;
    @Builder.Default
    private boolean isDefaultShipping = false;
    
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
    //relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;
}
