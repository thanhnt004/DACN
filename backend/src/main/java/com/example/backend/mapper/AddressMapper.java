package com.example.backend.mapper;

import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.model.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressMapper extends GenericMapper<Address, UserAddress> {
    @Override
    @Mapping(target = "isDefaultShipping",source = "defaultShipping")
    Address toEntity(UserAddress dto);

    @Override
    UserAddress toDto(Address entity);

    @Override
    List<UserAddress> toDto(List<Address> entity);

    @Override
    void updateFromDto(UserAddress dto,@MappingTarget Address entity);
}
