package com.example.backend.mapper;

import com.example.backend.dto.request.catalog.product.VariantCreateRequest;
import com.example.backend.dto.request.catalog.product.VariantUpdateRequest;
import com.example.backend.dto.response.catalog.product.VariantResponse;
import com.example.backend.model.product.Inventory;
import com.example.backend.model.product.ProductVariant;
import com.example.backend.model.product.Size_;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", uses = { InventoryMapper.class })
public interface ProductVariantMapper {
    @Mapping(target = "productId", source = "productVariant.product.id")
    @Mapping(target = "sizeId", source = "productVariant.size.id")
    @Mapping(target = "colorId", source = "productVariant.color.id")
    @Mapping(target = "size.id", source = "productVariant.size.id")
    @Mapping(target = "size.name", source = "productVariant.size.name")
    @Mapping(target = "size.code", source = "productVariant.size.code")
    @Mapping(target = "color.id", source = "productVariant.color.id")
    @Mapping(target = "color.name", source = "productVariant.color.name")
    @Mapping(target = "color.hexCode", source = "productVariant.color.hexCode")
    @Mapping(target = "inventory")
    VariantResponse toResponse(ProductVariant productVariant);
    List<VariantResponse> toResponse(List<ProductVariant> productVariant);

    @Mapping(target = "product", ignore = true) // Product will be set by service
    @Mapping(target = "size", ignore = true) // Size will be set by service
    @Mapping(target = "color", ignore = true) // Color will be set by service
    @Mapping(target = "inventory", ignore = true) // Inventory will be set by service
    @Mapping(target = "image", ignore = true) // Image will be set by service
    ProductVariant toEntity(VariantCreateRequest request);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "product", ignore = true) // Product cannot be changed
    @Mapping(target = "size", ignore = true) // Size will be set by service if needed
    @Mapping(target = "color", ignore = true) // Color will be set by service if needed
    @Mapping(target = "inventory", ignore = true) // Inventory handled separately
    @Mapping(target = "image", ignore = true) // Image handled separately
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateFromDto(@MappingTarget ProductVariant productVariant, VariantUpdateRequest request);
}
