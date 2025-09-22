package com.example.backend.mapper;

import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.model.User;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(RegisterRequest request);
}
