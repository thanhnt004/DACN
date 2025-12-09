package com.example.backend.mapper;

import com.example.backend.dto.request.catalog.product.ProductCreateRequest;
import com.example.backend.dto.request.catalog.product.ProductUpdateRequest;
import com.example.backend.dto.response.catalog.product.ProductDetailResponse;
import com.example.backend.model.product.Product;
import org.mapstruct.*;


@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = {BrandMapper.class})
public interface ProductMapper {
    @Mapping(target = "images",ignore = true)
    @Mapping(target = "brand", ignore = true)
    Product toEntity(ProductCreateRequest productCreateRequest);

    @Mapping(target = "images",ignore = true)
    @Mapping(target = "categories",ignore = true)
    @Mapping(target = "variants",ignore = true)
    @Mapping(target = "options",ignore = true)
    @Mapping(target = "brandId", source = "brand.id")
    ProductDetailResponse toDto(Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "images",ignore = true)
    @Mapping(target = "brand", ignore = true)
    void updateFromDto(ProductUpdateRequest dto, @MappingTarget Product product);

}
