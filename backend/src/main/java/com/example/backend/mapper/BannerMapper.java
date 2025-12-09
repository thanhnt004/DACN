package com.example.backend.mapper;

import com.example.backend.dto.request.banner.BannerCreateRequest;
import com.example.backend.dto.request.banner.BannerUpdateRequest;
import com.example.backend.dto.response.banner.BannerResponse;
import com.example.backend.model.banner.Banner;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BannerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Banner toEntity(BannerCreateRequest request);

    @Mapping(target = "isCurrentlyValid", expression = "java(banner.isCurrentlyValid())")
    BannerResponse toResponse(Banner banner);

    List<BannerResponse> toResponseList(List<Banner> banners);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateFromDto(BannerUpdateRequest request, @MappingTarget Banner banner);
}

