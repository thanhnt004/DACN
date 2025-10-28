package com.example.backend.repository.catalog.product;

import com.example.backend.model.product.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
}
