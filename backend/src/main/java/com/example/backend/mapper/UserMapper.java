package com.example.backend.mapper;

import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.model.User;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "passwordHash", ignore = true)
    User toUser(RegisterRequest request, @Context PasswordEncoder passwordEncoder);

    @AfterMapping
    default void encodePassword(@MappingTarget User.UserBuilder builder, RegisterRequest request, @Context PasswordEncoder encoder) {
        if (request.getPassword() != null) {
            builder.passwordHash(encoder.encode(request.getPassword()));
        }
    }
}
