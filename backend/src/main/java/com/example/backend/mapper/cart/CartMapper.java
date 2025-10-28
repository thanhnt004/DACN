package com.example.backend.mapper.cart;

import com.example.backend.dto.response.cart.CartResponse;
import com.example.backend.model.cart.Cart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper(componentModel = "spring", uses = { CartItemMapper.class })
public interface CartMapper {

    CartMapper INSTANCE = Mappers.getMapper(CartMapper.class);

    @Mapping(target = "items", source = "items")
    @Mapping(target = "cartStatus", source = "status")
    CartResponse toDto(Cart cart);

    default LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) return null;
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

}