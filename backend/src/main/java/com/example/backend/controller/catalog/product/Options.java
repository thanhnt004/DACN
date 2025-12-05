package com.example.backend.controller.catalog.product;

import com.example.backend.dto.response.catalog.BrandDto;
import com.example.backend.dto.response.catalog.ColorDto;
import com.example.backend.dto.response.catalog.SizeDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Options {
    private List<SizeDto> size;
    private List<ColorDto> color;
}
