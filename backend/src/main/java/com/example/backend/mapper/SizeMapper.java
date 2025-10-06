package com.example.backend.mapper;

import com.example.backend.dto.ColorDto;
import com.example.backend.dto.SizeDto;
import com.example.backend.model.product.Color;
import com.example.backend.model.product.Size;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface SizeMapper extends GenericMapper<Size, SizeDto> {
    @Override
    Size toEntity(SizeDto dto);
    @Override
    SizeDto toDto(Size size);
    @Override
    List<SizeDto> toDto(List<Size> sizes);
    @Override
    void updateFromDto(SizeDto sizeDto, @MappingTarget Size size);
}
