package com.example.backend.mapper.discount;

import com.example.backend.dto.response.discount.DiscountRedemptionResponse;
import com.example.backend.dto.response.discount.DiscountResponse;
import com.example.backend.model.DiscountRedemption;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DiscountRedemptionMapper {
    DiscountRedemptionResponse toDto(DiscountRedemption discountRedemption);
}
