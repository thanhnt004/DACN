package com.example.backend.mapper;

import com.example.backend.dto.request.product.VariantCreateRequest;
import com.example.backend.dto.response.product.VariantResponse;
import com.example.backend.dto.response.product.VariantSummaryResponse;
import com.example.backend.model.product.ProductVariant;
import jdk.dynalink.linker.LinkerServices;
import org.mapstruct.Mapper;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {
    VariantResponse toResponse(ProductVariant productVariant);
    VariantSummaryResponse toDto(ProductVariant entity);
    List<VariantSummaryResponse> toDto(List<ProductVariant> entity);


    ProductVariant toEntity(VariantCreateRequest request);
}
