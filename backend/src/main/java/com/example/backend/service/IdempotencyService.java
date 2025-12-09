package com.example.backend.service;

import com.example.backend.model.IdempotencyKey;
import com.example.backend.repository.IdempotencyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyKey getRecord(String key) {
        return idempotencyRepository.findByKeyValue(key).orElse(null);
    }


    @Transactional
    public void saveSuccess(String key, Object result) throws JsonProcessingException {
        HttpStatusCode status = HttpStatus.OK;
        Object body = result;

        if (result instanceof ResponseEntity) {
            ResponseEntity<?> resp = (ResponseEntity<?>) result;
            status = resp.getStatusCode();
            body = resp.getBody();
        }

        // Convert body to Map for storage
        @SuppressWarnings("unchecked")
        Map<String, Object> bodyMap = objectMapper.convertValue(body, Map.class);

        Instant now = Instant.now();
        IdempotencyKey record = IdempotencyKey.builder()
                .keyValue(key)
                .status(IdempotencyKey.Status.SUCCESS)
                .responseBody(bodyMap)
                .createdAt(now)
                .expiresAt(now.plusSeconds(3600)) // 1 hour expiry for success records
                .build();
        idempotencyRepository.save(record);
    }

    @Transactional
    public void delete(String key) {
        idempotencyRepository.deleteByKeyValue(key);
    }

    @Transactional
    public void saveProcessing(String key, String hashId, long expireTime) {
        Instant now = Instant.now();
        IdempotencyKey record = IdempotencyKey.builder()
                .keyValue(key)
                .hash(hashId)
                .status(IdempotencyKey.Status.PROCESSING)
                .createdAt(now)
                .expiresAt(now.plusSeconds(expireTime))
                .build();
        idempotencyRepository.save(record);
    }
}
