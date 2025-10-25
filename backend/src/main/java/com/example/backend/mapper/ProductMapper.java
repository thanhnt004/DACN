package com.example.backend.mapper;

import com.example.backend.dto.request.catalog.product.ProductCreateRequest;
import com.example.backend.dto.request.catalog.product.ProductUpdateRequest;
import com.example.backend.dto.response.catalog.product.ProductDetailResponse;
import com.example.backend.model.product.Product;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "images",ignore = true)
    Product toEntity(ProductCreateRequest productCreateRequest);

    @Mapping(target = "images",ignore = true)
    @Mapping(target = "categories",ignore = true)
    @Mapping(target = "variants",ignore = true)
    @Mapping(target = "options",ignore = true)
    ProductDetailResponse toDto(Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "images",ignore = true)
    void updateFromDto(ProductUpdateRequest dto, @MappingTarget Product product);

}
