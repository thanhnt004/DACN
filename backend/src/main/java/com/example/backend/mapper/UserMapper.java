package com.example.backend.mapper;


import com.example.backend.dto.request.auth.RegisterRequest;
import com.example.backend.dto.response.user.UserProfileDto;
import com.example.backend.model.User;
import org.mapstruct.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "passwordHash", ignore = true)
    User toUser(RegisterRequest request, @Context PasswordEncoder passwordEncoder);

    @AfterMapping
    default void encodePassword(@MappingTarget User.UserBuilder builder, RegisterRequest request, @Context PasswordEncoder encoder) {
        if (request.getPassword() != null) {
            builder.passwordHash(encoder.encode(request.getPassword()));
        }
    }
    @Mapping(target = "isActive", expression = "java(currentUser.isActive())")
    UserProfileDto toUserProfile(User currentUser);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(@MappingTarget User user, UserProfileDto dto);
}
