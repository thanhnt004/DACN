package com.example.backend.mapper;


import com.example.backend.dto.request.catalog.product.ProductImageRequest;
import com.example.backend.dto.response.catalog.product.ProductImageResponse;
import com.example.backend.model.product.ProductImage;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    ProductImage toEntity(ProductImageRequest dto);

    ProductImageResponse toDto(ProductImage images);
    List<ProductImageResponse> toDto(List<ProductImage> images);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(@MappingTarget ProductImage productImage, ProductImageRequest request);
}
