package com.example.backend.mapper;

import com.example.backend.dto.request.catalog.product.InventoryRequest;
import com.example.backend.dto.response.catalog.product.InventoryResponse;
import com.example.backend.model.product.Inventory;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    InventoryResponse toDto(Inventory inventory);
    Inventory toEntity(InventoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",ignore = true)
    void updateFromDto(@MappingTarget Inventory inventory,InventoryRequest request);
}
