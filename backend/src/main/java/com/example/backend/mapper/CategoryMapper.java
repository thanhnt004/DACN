package com.example.backend.mapper;

import com.example.backend.dto.request.catalog.category.CategoryCreateRequest;
import com.example.backend.dto.request.catalog.category.CategoryUpdateRequest;
import com.example.backend.dto.response.catalog.category.CategoryResponse;
import com.example.backend.model.product.Category;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "children", ignore = true)
    @Mapping(target = "productsCount", ignore = true)
    CategoryResponse toDto(Category entity);

    List<CategoryResponse> toDto(List<Category> entities);

    // mapping từ request -> entity (dùng khi create)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    Category toEntity(CategoryCreateRequest req);

    // update: dùng @MappingTarget
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "parent", ignore = true)
    void updateFromDto(CategoryUpdateRequest req, @MappingTarget Category entity);

}
