package com.example.backend.controller.catalog.product;

import com.example.backend.dto.response.catalog.BrandDto;
import com.example.backend.dto.response.catalog.ColorDto;
import com.example.backend.dto.response.catalog.SizeDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Options {
    private List<SizeDto> size;
    private List<ColorDto> color;
}
