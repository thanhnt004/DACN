package com.example.backend.mapper;

import com.example.backend.dto.BrandDto;
import com.example.backend.model.product.Brand;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrandMapper {
    BrandDto toDto(Brand brand);

    List<BrandDto> toDto(List<Brand> brands);

    Brand toEntity(BrandDto brandDto);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(@MappingTarget Brand brand,BrandDto brandDto);
}
