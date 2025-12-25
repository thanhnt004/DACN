package com.example.backend.mapper;

import com.example.backend.dto.request.catalog.product.ProductImageRequest;
import com.example.backend.dto.response.catalog.product.ProductImageResponse;
import com.example.backend.model.product.ProductImage;
import com.example.backend.model.product.ProductVariant;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class ImageMapper {

    @Autowired
    protected ProductVariantRepository variantRepository;

    @Mapping(target = "isDefault", source = "default")
    @Mapping(target = "variant", expression = "java(mapVariant(dto.getVariantId()))")
    public abstract ProductImage toEntity(ProductImageRequest dto);

    @Mapping(target = "isDefault", source = "default")
    @Mapping(target = "variantId", source = "variant.id")
    public abstract ProductImageResponse toDto(ProductImage entity);
    
    public abstract List<ProductImageResponse> toDto(List<ProductImage> images);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateFromDto(@MappingTarget ProductImage productImage, ProductImageRequest request);

    protected ProductVariant mapVariant(UUID variantId) {
        if (variantId == null) {
            return null;
        }
        return variantRepository.findById(variantId).orElse(null);
    }
}
