package com.example.backend.controller.catalog.category;

import com.example.backend.dto.request.catalog.category.CategoryCreateRequest;
import com.example.backend.dto.request.catalog.category.CategoryUpdateRequest;
import com.example.backend.dto.request.catalog.product.MoveCategoryRequest;
import com.example.backend.dto.response.catalog.category.CategoryResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.service.product.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    @GetMapping("/{slugOrId}")
    public ResponseEntity<CategoryResponse> getBySlugOrId(@PathVariable String slugOrId)
    {
        CategoryResponse categoryDto = categoryService.getBySlugOrId(slugOrId);
        return ResponseEntity.ok(categoryDto);
    }
    @GetMapping
    public ResponseEntity<CategoryResponse> getCategoryTree(
            @RequestParam(required = false) UUID rootId,
            @RequestParam(defaultValue = "3") int depth
    )
    {
        CategoryResponse dto = categoryService.getCategoryTree(depth,rootId);
        return ResponseEntity.ok(dto);
    }
    @GetMapping("/flat")
    public ResponseEntity<PageResponse<CategoryResponse>> getCategoriesFlat(@RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "20") int size,
                                                                            @RequestParam(required = false) String search,
                                                                            @RequestParam(required = false) UUID parentId,@RequestParam(required = false) String sort)
    {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<CategoryResponse> categories = categoryService.list(parentId,search,pageable,sort);
        return ResponseEntity.ok(categories);
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {

        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(category);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryUpdateRequest request) {

        CategoryResponse category = categoryService.update(id, request);
        return ResponseEntity.ok(category);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<String> delete(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") Boolean force,
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
    ){
        categoryService.move(id, request.getNewParentId());
        return ResponseEntity.noContent().build();
    }
}
