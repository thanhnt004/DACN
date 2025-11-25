package com.example.backend.mapper.cart;

import com.example.backend.dto.response.cart.CartResponse;
import com.example.backend.model.cart.Cart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = { CartItemMapper.class })
public interface CartMapper {

    @Mapping(target = "items", source = "items")
    @Mapping(target = "cartStatus", source = "status")
    CartResponse toDto(Cart cart);
}
