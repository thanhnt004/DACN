package com.example.backend.repository;

import com.example.backend.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByKeyValue(String keyValue);

    void deleteByKeyValue(String keyValue);
}
