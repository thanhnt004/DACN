package com.example.backend.mapper.cart;

import com.example.backend.dto.response.cart.CartItemResponse;
import com.example.backend.model.cart.CartItem;
import com.example.backend.model.product.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    CartItemMapper INSTANCE = Mappers.getMapper(CartItemMapper.class);

    @Mapping(target = "productId", source = "variant.product.id")
    @Mapping(target = "productName", source = "variant.product.name")
    @Mapping(target = "variantId", source = "variant.id")
    // buildVariantName bằng expression gọi method helper
    @Mapping(target = "variantName", expression = "java(buildVariantName(cartItem.getVariant()))")
    @Mapping(target = "imageUrl", source = "variant.product.primaryImageUrl")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "stockQuantity", source = "variant.inventory.availableStock")
    @Mapping(target = "unitPriceAmount", source = "unitPriceAmount")
    // isInStock tính dựa vào availableStock
    @Mapping(target = "isInStock", expression = "java(isInStock(cartItem))")
    CartItemResponse toDto(CartItem cartItem);

    // helper default method: build variant name
    default String buildVariantName(ProductVariant variant) {
        if (variant == null) return null;

        StringBuilder sb = new StringBuilder();
        if (variant.getColor() != null && variant.getColor().getName() != null) {
            sb.append(variant.getColor().getName());
        }
        if (variant.getSize() != null && variant.getSize().getName() != null) {
            if (!sb.isEmpty()) sb.append(" / ");
            sb.append(variant.getSize().getName());
        }
        if (sb.isEmpty()) {
            return variant.getSku();
        }
        return sb.toString();
    }

    default boolean isInStock(CartItem cartItem) {
        if (cartItem == null) return false;
        ProductVariant v = cartItem.getVariant();
        if (v == null || v.getInventory() == null) return false;
        return v.getInventory().getAvailableStock() > 0;
    }
}
