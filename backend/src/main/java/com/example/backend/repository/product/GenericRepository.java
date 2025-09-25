package com.example.backend.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GenericRepository<T> extends JpaRepository<T, UUID> {
    Optional<T> findById(UUID uuid);
}
