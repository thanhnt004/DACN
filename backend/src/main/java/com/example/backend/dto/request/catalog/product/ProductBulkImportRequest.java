package com.example.backend.dto.request.catalog.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ProductBulkImportRequest {
    // Product Information
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotBlank(message = "Slug không được để trống")
    private String slug;

    private String description;
    private String material;

    @Pattern(regexp = "men|women|unisex", message = "Giới tính phải là men, women hoặc unisex")
    private String gender = "unisex";

    private String brandName; // Brand name instead of ID for easier Excel input
    private List<String> categoryNames; // Category names instead of IDs

    // SEO
    private String seoTitle;
    private String seoDescription;

    // Variant Information
    @NotBlank(message = "SKU không được để trống")
    private String sku;

    private String barcode;
    private String sizeName; // Size name for easier input
    private String colorName; // Color name for easier input
    private String colorHex; // Color hex code

    @NotNull(message = "Giá bán không được null")
    @Positive(message = "Giá bán phải lớn hơn 0")
    private Long priceAmount;

    private Long compareAtAmount;
    private Long historyCost;
    private Integer weightGrams = 200;

    // Inventory Information
    @NotNull(message = "Số lượng tồn kho không được null")
    private Integer quantityOnHand = 0;

    private Integer quantityReserved = 0;
    private Integer reorderLevel = 5;

    // Image Information
    private String imageUrl;
    private String imageAlt;
    private Boolean isDefaultImage = false;
    private Integer imagePosition = 0;
}
