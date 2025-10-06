package com.example.backend.mapper;

import com.example.backend.dto.request.product.ProductCreateRequest;
import com.example.backend.dto.request.product.ProductUpdateRequest;
import com.example.backend.dto.response.product.ProductResponse;
import com.example.backend.model.product.Product;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "images",ignore = true)
    Product toEntity(ProductCreateRequest productCreateRequest);

    @Mapping(target = "images",ignore = true)
    @Mapping(target = "categories",ignore = true)
    @Mapping(target = "variants",ignore = true)
    ProductResponse toDto(Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "images",ignore = true)
    void updateFromDto(ProductUpdateRequest dto, @MappingTarget Product product);

}
