package com.example.backend.service.user;

import com.example.backend.dto.response.user.UserAuthenDTO;
import com.example.backend.exception.user.InvalidIdentifierException;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final com.example.backend.config.CacheConfig cacheConfig;

    @Cacheable(value = "#{@cacheConfig.userCache}", key = "'auth:' + #identifier")
    public UserAuthenDTO loadUserAuthFromDb(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findAuthByEmail(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("Email not found!"));
        } else if (identifier.matches("\\d+")) {
            return userRepository.findAuthByPhone(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("Phone number not found!"));
        } else {
            throw new InvalidIdentifierException();
        }
    }

    /**
     * Evict user cache when user data changes
     */
    @CacheEvict(value = "#{@cacheConfig.userCache}", key = "'auth:' + #identifier")
    public void evictUserCache(String identifier) {
        // Cache will be evicted automatically
    }

    /**
     * Evict all user cache entries
     */
    @CacheEvict(value = "#{@cacheConfig.userCache}", allEntries = true)
    public void evictAllUserCache() {
        // All cache entries will be evicted
    }
}
