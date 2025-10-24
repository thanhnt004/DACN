package com.example.backend.dto.response.auth;

import com.example.backend.dto.response.user.UserAuthenDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CustomUserDetail implements UserDetails, CredentialsContainer {
    private UUID id;
    private String email;
    private List<SimpleGrantedAuthority> authorities;
    private String passwordHash;
    private boolean locked;
    private boolean enabled;
    public static CustomUserDetail createUserDetail(UserAuthenDTO userAuthenDTO)
    {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_"+userAuthenDTO.getRole()));
        return new CustomUserDetail(userAuthenDTO.getId(),userAuthenDTO.getEmail(),authorities,userAuthenDTO.getPasswordHash(), userAuthenDTO.isLocked(), userAuthenDTO.isEnabled());
    }
    public CustomUserDetail() {}
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
