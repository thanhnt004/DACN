package com.example.backend.mapper;

import com.example.backend.dto.ProductImageDto;
import com.example.backend.dto.response.product.ProductImageResponse;
import com.example.backend.model.product.ProductImage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    ProductImage toEntity(ProductImageDto dto);

    ProductImageResponse toDto(ProductImage images);
    List<ProductImageResponse> toDto(List<ProductImage> images);
}
