package com.example.backend.mapper;

import com.example.backend.dto.request.catalog.product.InventoryRequest;
import com.example.backend.dto.response.catalog.product.InventoryResponse;
import com.example.backend.model.product.Inventory;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    @Mapping(target = "variantId", expression = "java(inventory.getVariant().getId())")
    @Mapping(target = "available", expression = "java(inventory.getQuantityOnHand() - inventory.getQuantityReserved())")
    InventoryResponse toDto(Inventory inventory);

    @Mapping(target = "id",ignore = true)
    @Mapping(target = "variant",ignore = true)
    @Mapping(target = "updatedAt",ignore = true)
    Inventory toEntity(InventoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",ignore = true)
    void updateFromDto(@MappingTarget Inventory inventory,InventoryRequest request);
}
