package com.example.backend.dto.response.user;

import com.example.backend.model.enumrator.Role;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuthenDTO {
    private UUID id;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean locked;
    private boolean enabled;
}
