package com.example.backend.service;

import com.example.backend.model.IdempotencyKey;
import com.example.backend.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyRepository idempotencyRepository;

    public IdempotencyKey getRecord(String key) {
        return idempotencyRepository.findByKeyValue(key).orElse(null);
    }


    public void saveSuccess(String key, Object result) {
        IdempotencyKey record = IdempotencyKey.builder()
                .keyValue(key)
                .status(IdempotencyKey.Status.SUCCESS)
                .responseBody((Map<String, Object>) result)
                .build();
        idempotencyRepository.save(record);
    }

    public void delete(String key) {
        idempotencyRepository.deleteByKeyValue(key);
    }

    public void saveProcessing(String key, String hashId, long expireTime) {
        IdempotencyKey record = IdempotencyKey.builder()
                .keyValue(key)
                .hash(hashId)
                .status(IdempotencyKey.Status.PROCESSING)
                .expiresAt(Instant.now().plusSeconds(expireTime))
                .build();
        idempotencyRepository.save(record);
    }
}
