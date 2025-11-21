package com.example.backend.mapper;

import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.model.Address;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressMapper extends GenericMapper<Address, UserAddress> {
    @Override
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Address toEntity(UserAddress dto);

    @Override
    UserAddress toDto(Address entity);

    @Override
    List<UserAddress> toDto(List<Address> entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromDto(UserAddress dto, @MappingTarget Address entity);
}
