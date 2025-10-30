package com.example.backend.controller.discount;

import com.example.backend.dto.request.discount.*;
import com.example.backend.dto.response.discount.DiscountRedemptionResponse;
import com.example.backend.dto.response.discount.DiscountResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.service.discount.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/discounts")
public class DiscountController {
    private final DiscountService discountService;
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DiscountResponse> createDiscount(@RequestBody @Valid DiscountCreateRequest request) {
        DiscountResponse createdDiscount = discountService.create(request);
        return ResponseEntity.ok(createdDiscount);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DiscountResponse> updateDiscount(@PathVariable UUID id, @Valid @RequestBody DiscountUpdateRequest request) {
        DiscountResponse updatedDiscount = discountService.update(id, request);
        return ResponseEntity.ok(updatedDiscount);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiscountResponse> getDiscountById(@PathVariable UUID id) {
        DiscountResponse discount = discountService.getById(id);
        return ResponseEntity.ok(discount);
    }

    @GetMapping
    public ResponseEntity<PageResponse<DiscountResponse>> listDiscounts(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20,  sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<DiscountResponse> discounts = discountService.list(code, active, pageable);
        return ResponseEntity.ok(discounts);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiscount(@PathVariable UUID id) {
        discountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Quản lý Sản phẩm áp dụng ---

    @PostMapping("/{id}/products")
    public ResponseEntity<Void> addProductsToDiscount(@PathVariable UUID id, @Valid @RequestBody ProductAssignmentRequest request) {
        discountService.addProducts(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/products")
    public ResponseEntity<Void> removeProductsFromDiscount(@PathVariable UUID id, @Valid @RequestBody ProductAssignmentRequest request) {
        discountService.removeProducts(id, request);
        return ResponseEntity.noContent().build();
    }

    // --- Quản lý Danh mục áp dụng ---

    @PostMapping("/{id}/categories")
    public ResponseEntity<Void> addCategoriesToDiscount(@PathVariable UUID id, @Valid @RequestBody CategoryAssignmentRequest request) {
        discountService.addCategories(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/categories")
    public ResponseEntity<Void> removeCategoriesFromDiscount(@PathVariable UUID id, @Valid @RequestBody CategoryAssignmentRequest request) {
        discountService.removeCategories(id, request);
        return ResponseEntity.noContent().build();
    }

    // --- Xem lịch sử sử dụng ---

    @GetMapping("/{id}/redemptions")
    public ResponseEntity<PageResponse<DiscountRedemptionResponse>> getDiscountRedemptions(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {

        PageResponse<DiscountRedemptionResponse> redemptions = discountService.getRedemptions(id, pageable);
        return ResponseEntity.ok(redemptions);
    }
}
