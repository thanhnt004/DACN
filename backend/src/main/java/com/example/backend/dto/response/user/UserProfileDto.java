package com.example.backend.dto.response.user;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileDto {
    UUID id;
    String fullName;
    String phone;
    String avatarUrl;
    LocalDateTime passwordChangedAt;
}
