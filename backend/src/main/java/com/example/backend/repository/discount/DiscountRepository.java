package com.example.backend.repository.discount;

import com.example.backend.model.discount.Discount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discount, UUID> {

}
