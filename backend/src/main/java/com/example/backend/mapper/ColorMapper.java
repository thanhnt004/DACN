package com.example.backend.mapper;

import com.example.backend.dto.ColorDto;
import com.example.backend.model.product.Color;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ColorMapper extends GenericMapper<Color, ColorDto> {
    @Override
    Color toEntity(ColorDto dto);
    @Override
    ColorDto toDto(Color color);
    @Override
    List<ColorDto> toDto(List<Color> colors);
    @Override
    void updateFromDto(ColorDto colorDto, @MappingTarget Color color);
}
