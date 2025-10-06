package com.example.backend.controller.product;

import com.example.backend.dto.request.product.CategoryCreateRequest;
import com.example.backend.dto.request.product.CategoryUpdateRequest;
import com.example.backend.dto.request.product.MoveCategoryRequest;
import com.example.backend.dto.response.product.CategoryDto;
import com.example.backend.service.product.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getById(@PathVariable UUID id)
    {
        CategoryDto categoryDto = categoryService.getById(id);
        return ResponseEntity.ok(categoryDto);
    }
    @GetMapping
    public ResponseEntity<CategoryDto> getAllCategories(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "null") UUID rootId,
            @RequestParam(defaultValue = "3") int depth
    )
    {
        CategoryDto dto = categoryService.getCategoryTree(includeInactive,depth,rootId);
        return ResponseEntity.ok(dto);
    }
    @GetMapping("/flat")
    public ResponseEntity<Page<CategoryDto>> getCategoriesFlat(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size,
                                                               @RequestParam(required = false) String search,
                                                               @RequestParam(required = false) Boolean isDeleted,
                                                               @RequestParam(required = false) UUID parentId)
    {
        Pageable pageable = PageRequest.of(page, size);
        Page<CategoryDto> categories = categoryService.list(parentId, isDeleted,search,pageable);
        return ResponseEntity.ok(categories);
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<CategoryDto> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {

        CategoryDto category = categoryService.createCateGory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(category);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryUpdateRequest request) {

        CategoryDto category = categoryService.update(id, request);
        return ResponseEntity.ok(category);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<String> delete(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean force,
            @RequestParam(required = false) UUID reassignTo
    )
    {
        categoryService.delete(id,force,reassignTo);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}/move")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Void> moveCategory(
            @PathVariable UUID id,
            @RequestBody @Valid MoveCategoryRequest request
    ) {
        categoryService.move(id, request.getNewParentId());
        return ResponseEntity.noContent().build();
    }

}
