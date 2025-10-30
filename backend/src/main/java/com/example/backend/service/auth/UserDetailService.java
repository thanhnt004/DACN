package com.example.backend.service.auth;

import com.example.backend.dto.response.auth.CustomUserDetail;
import com.example.backend.dto.response.user.UserAuthenDTO;
import com.example.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailService implements UserDetailsService {
    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        UserAuthenDTO userAuthenDTO = userService.loadUserAuthFromDb(identifier);
        log.info(userAuthenDTO.getId().toString());
        return CustomUserDetail.createUserDetail(userAuthenDTO);
    }
}
