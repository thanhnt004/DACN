package com.example.backend.service.redis;

import com.example.backend.dto.response.checkout.CheckoutSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Service quản lý checkout sessions trong Redis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCheckoutStoreService {

    private static final String SESSION_KEY_PREFIX = "checkout:session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, CheckoutSession> redisTemplate;

    /**
     * Lưu session vào Redis với TTL
     */
    public void save(CheckoutSession session) {
        String key = getKey(String.valueOf(session.getId()));
        redisTemplate.opsForValue().set(key, session, SESSION_TTL);
        log.debug("Saved session to Redis: {}", session.getId());
    }

    /**
     * Lấy session từ Redis
     */
    public Optional<CheckoutSession> findById(UUID sessionId) {
        String key = getKey(sessionId.toString());
        CheckoutSession session = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(session);
    }

    /**
     * Xóa session khỏi Redis
     */
    public void delete(UUID sessionId) {
        String key = getKey(String.valueOf(sessionId));
        redisTemplate.delete(key);
        log.debug("Deleted session from Redis: {}", sessionId);
    }

    /**
     * Gia hạn TTL của session (refresh khi có activity)
     */
    public void refreshTTL(UUID sessionId) {
        String key = getKey(String.valueOf(sessionId));
        redisTemplate.expire(key, SESSION_TTL);
        log.debug("Refreshed TTL for session: {}", sessionId);
    }

    /**
     * Kiểm tra session có tồn tại không
     */
    public boolean exists(String sessionId) {
        String key = getKey(sessionId);
        return redisTemplate.hasKey(key);
    }

    private String getKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }
}