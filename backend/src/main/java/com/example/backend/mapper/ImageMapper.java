package com.example.backend.mapper;


import com.example.backend.dto.request.catalog.product.ProductImageRequest;
import com.example.backend.dto.response.catalog.product.ProductImageResponse;
import com.example.backend.model.product.ProductImage;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    @Mapping(target = "isDefault", source = "default")
    ProductImage toEntity(ProductImageRequest dto);

    @Mapping(target = "isDefault", source = "default")
    ProductImageResponse toDto(ProductImage images);
    List<ProductImageResponse> toDto(List<ProductImage> images);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(@MappingTarget ProductImage productImage, ProductImageRequest request);
}
