package com.example.backend.service.user;

import com.example.backend.dto.response.user.UserAuthenDTO;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    @Cacheable(value = "users", key = "#identifier")
    public UserAuthenDTO loadUserAuthFromDb(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findAuthByEmail(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("Email not found!"));
        } else if (identifier.matches("\\d+")) {
            return userRepository.findAuthByPhone(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException("Phone number not found!"));
        } else {
            throw new IllegalArgumentException("Invalid identifier");
        }
    }
}
