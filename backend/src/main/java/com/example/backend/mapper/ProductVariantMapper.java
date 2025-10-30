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
    @Mapping(target = "productId",source = "productVariant.product.id")
    @Mapping(target = "sizeId",source = "productVariant.size.id")
    @Mapping(target = "colorId",source = "productVariant.color.id")
    @Mapping(target = "inventory")
    VariantResponse toResponse(ProductVariant productVariant);
    List<VariantResponse> toResponse(List<ProductVariant> productVariant);

    ProductVariant toEntity(VariantCreateRequest request);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(@MappingTarget ProductVariant productVariant, VariantUpdateRequest request);
}
