package com.example.backend.dto.response.user;

import com.example.backend.model.User;
import com.example.backend.model.enumrator.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserProfileDto {
    UUID id;
    String fullName;
    String email;
    String phone;
    String avatarUrl;
    User.Gender gender = User.Gender.O;
    LocalDate dateOfBirth;
    Instant passwordChangedAt;
    Role role;
    Boolean isActive;
}
