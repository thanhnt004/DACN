package com.example.backend.util;

import com.example.backend.dto.response.auth.CustomUserDetail;
import com.example.backend.model.User;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenUtil {
    private final UserRepository userRepository;
    public Optional<User> getAuthenUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> user = Optional.empty();
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof CustomUserDetail userDetail) {
            user = userRepository.findById(((CustomUserDetail) authentication.getPrincipal()).getId());
        }
        return user;
    }
}
