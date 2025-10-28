package com.example.backend.mapper.discount;

import com.example.backend.dto.request.discount.DiscountCreateRequest;
import com.example.backend.dto.request.discount.DiscountUpdateRequest;
import com.example.backend.dto.response.discount.DiscountResponse;
import com.example.backend.model.discount.Discount;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DiscountMapper {
    Discount toEntity(DiscountCreateRequest request);
    DiscountResponse toDto(Discount discount);

    @BeanMapping(nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(DiscountUpdateRequest request,@MappingTarget Discount discount);
}
