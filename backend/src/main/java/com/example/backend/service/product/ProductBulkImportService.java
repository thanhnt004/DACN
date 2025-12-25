package com.example.backend.service.product;

import com.example.backend.dto.request.catalog.product.ProductBulkImportRequest;
import com.example.backend.dto.request.catalog.product.ProductCreateRequest;
import com.example.backend.dto.request.catalog.product.VariantCreateRequest;
import com.example.backend.dto.request.catalog.product.InventoryRequest;
import com.example.backend.dto.request.catalog.product.ProductImageRequest;
import com.example.backend.dto.response.catalog.product.ProductBulkImportResponse;
import com.example.backend.model.product.Brand;
import com.example.backend.model.product.Category;
import com.example.backend.model.product.Product;
import com.example.backend.model.product.Size;
import com.example.backend.model.product.Color;
import com.example.backend.repository.catalog.brand.BrandRepository;
import com.example.backend.repository.catalog.categoty.CategoryRepository;
import com.example.backend.repository.catalog.product.ColorRepository;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.repository.catalog.product.SizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductBulkImportService {

    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final ProductRepository productRepository;
    private final PlatformTransactionManager transactionManager;

    public ProductBulkImportResponse importFromExcel(MultipartFile file) throws IOException {
        List<ProductBulkImportRequest> importRequests = parseExcelFile(file);
        return processImportRequests(importRequests);
    }

    private List<ProductBulkImportRequest> parseExcelFile(MultipartFile file) throws IOException {
        List<ProductBulkImportRequest> requests = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    ProductBulkImportRequest request = parseRowToRequest(row);
                    if (request != null) {
                        requests.add(request);
                    }
                } catch (Exception e) {
                    log.error("Error parsing row {}: {}", i + 1, e.getMessage());
                    // Continue with other rows
                }
            }
        }

        return requests;
    }

    private ProductBulkImportRequest parseRowToRequest(Row row) {
        if (isEmptyRow(row)) return null;

        ProductBulkImportRequest request = new ProductBulkImportRequest();

        // Product info
        request.setName(getCellValueAsString(row.getCell(0)));
        request.setSlug(getCellValueAsString(row.getCell(1)));
        request.setDescription(getCellValueAsString(row.getCell(2)));
        request.setMaterial(getCellValueAsString(row.getCell(3)));
        request.setGender(getCellValueAsString(row.getCell(4)));
        request.setBrandName(getCellValueAsString(row.getCell(5)));

        // Categories (comma-separated)
        String categoriesStr = getCellValueAsString(row.getCell(6));
        if (categoriesStr != null && !categoriesStr.trim().isEmpty()) {
            request.setCategoryNames(Arrays.stream(categoriesStr.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList()));
        }

        // SEO
        request.setSeoTitle(getCellValueAsString(row.getCell(7)));
        request.setSeoDescription(getCellValueAsString(row.getCell(8)));

        // Variant info
        request.setSku(getCellValueAsString(row.getCell(9)));
        request.setBarcode(getCellValueAsString(row.getCell(10)));
        request.setSizeName(getCellValueAsString(row.getCell(11)));
        request.setColorName(getCellValueAsString(row.getCell(12)));
        request.setColorHex(getCellValueAsString(row.getCell(13)));

        // Pricing
        request.setPriceAmount(getCellValueAsLong(row.getCell(14)));
        request.setCompareAtAmount(getCellValueAsLong(row.getCell(15)));
        request.setHistoryCost(getCellValueAsLong(row.getCell(16)));
        request.setWeightGrams(getCellValueAsInteger(row.getCell(17)));

        // Inventory
        request.setQuantityOnHand(getCellValueAsInteger(row.getCell(18)));
        request.setQuantityReserved(getCellValueAsInteger(row.getCell(19)));
        request.setReorderLevel(getCellValueAsInteger(row.getCell(20)));

        // Image
        request.setImageUrl(getCellValueAsString(row.getCell(21)));
        request.setImageAlt(getCellValueAsString(row.getCell(22)));
        request.setIsDefaultImage(getCellValueAsBoolean(row.getCell(23)));
        request.setImagePosition(getCellValueAsInteger(row.getCell(24)));

        return request;
    }

    private ProductBulkImportResponse processImportRequests(List<ProductBulkImportRequest> requests) {
        List<ProductBulkImportResponse.ImportResultItem> results = new ArrayList<>();
        List<String> globalErrors = new ArrayList<>();

        // Group requests by product (same name/slug)
        Map<String, List<ProductBulkImportRequest>> productGroups = requests.stream()
                .collect(Collectors.groupingBy(req -> req.getName() + "|" + req.getSlug()));

        int successCount = 0;
        int errorCount = 0;
        int rowNumber = 1; // Start from 1 (header is 0)

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (Map.Entry<String, List<ProductBulkImportRequest>> entry : productGroups.entrySet()) {
            List<ProductBulkImportRequest> productRequests = entry.getValue();
            int currentRowNumber = rowNumber; // Capture for lambda

            try {
                ProductBulkImportResponse.ImportResultItem result = transactionTemplate.execute(status -> {
                    return processProductGroup(productRequests, currentRowNumber);
                });
                
                if (result != null) {
                    results.add(result);
                    if ("SUCCESS".equals(result.getStatus())) {
                        successCount++;
                    } else {
                        errorCount++;
                    }
                }

            } catch (Exception e) {
                log.error("Error processing product group {}: {}", entry.getKey(), e.getMessage(), e);

                ProductBulkImportResponse.ImportResultItem errorResult = ProductBulkImportResponse.ImportResultItem.builder()
                        .rowNumber(rowNumber)
                        .status("ERROR")
                        .productName(productRequests.get(0).getName())
                        .message("Lỗi xử lý sản phẩm: " + e.getMessage())
                        .errors(Arrays.asList(e.getMessage()))
                        .build();

                results.add(errorResult);
                errorCount++;
            }

            rowNumber += productRequests.size();
        }

        return ProductBulkImportResponse.builder()
                .totalRows(requests.size())
                .successCount(successCount)
                .errorCount(errorCount)
                .results(results)
                .errors(globalErrors)
                .build();
    }

    public ProductBulkImportResponse.ImportResultItem processProductGroup(List<ProductBulkImportRequest> requests, int startRowNumber) {
        ProductBulkImportRequest firstRequest = requests.get(0);
        List<String> errors = new ArrayList<>();

        try {
            // Validate and get/create related entities
            Brand brand = findOrCreateBrand(firstRequest.getBrandName());
            List<Category> categories = findOrCreateCategories(firstRequest.getCategoryNames());

            // Check if product already exists
            Optional<Product> existingProduct = productRepository.findBySlug(firstRequest.getSlug());

            UUID productId;
            if (existingProduct.isPresent()) {
                productId = existingProduct.get().getId();
                log.info("Product already exists, adding variants to existing product: {}", firstRequest.getName());
            } else {
                // Create new product
                ProductCreateRequest productCreateRequest = buildProductCreateRequest(firstRequest, brand, categories);
                productId = productService.create(productCreateRequest).getId();
            }

            // Create variants for this product
            List<UUID> variantIds = new ArrayList<>();
            for (ProductBulkImportRequest request : requests) {
                try {
                    UUID variantId = createVariant(request, productId);
                    variantIds.add(variantId);
                } catch (Exception e) {
                    errors.add("Lỗi tạo variant " + request.getSku() + ": " + e.getMessage());
                    log.error("Error creating variant {}: {}", request.getSku(), e.getMessage(), e);
                }
            }

            String status = errors.isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS";
            String message = errors.isEmpty() ?
                    "Tạo thành công sản phẩm với " + variantIds.size() + " variant(s)" :
                    "Tạo sản phẩm thành công nhưng có " + errors.size() + " lỗi khi tạo variant";

            return ProductBulkImportResponse.ImportResultItem.builder()
                    .rowNumber(startRowNumber)
                    .status(status)
                    .productName(firstRequest.getName())
                    .sku(firstRequest.getSku())
                    .productId(productId)
                    .message(message)
                    .errors(errors.isEmpty() ? null : errors)
                    .build();

        } catch (Exception e) {
            log.error("Error creating product {}: {}", firstRequest.getName(), e.getMessage(), e);

            return ProductBulkImportResponse.ImportResultItem.builder()
                    .rowNumber(startRowNumber)
                    .status("ERROR")
                    .productName(firstRequest.getName())
                    .sku(firstRequest.getSku())
                    .message("Lỗi tạo sản phẩm: " + e.getMessage())
                    .errors(Arrays.asList(e.getMessage()))
                    .build();
        }
    }

    private UUID createVariant(ProductBulkImportRequest request, UUID productId) {
        VariantCreateRequest variantRequest = VariantCreateRequest.builder()
                .sku(request.getSku())
                .barcode(request.getBarcode())
                .priceAmount(request.getPriceAmount())
                .compareAtAmount(request.getCompareAtAmount())
                .historyCost(request.getHistoryCost())
                .weightGrams(request.getWeightGrams())
                .status("ACTIVE")
                .build();

        // Set size and color
        if (request.getSizeName() != null && !request.getSizeName().trim().isEmpty()) {
            Size size = findOrCreateSize(request.getSizeName().trim());
            variantRequest.setSizeId(size.getId());
        }

        if (request.getColorName() != null && !request.getColorName().trim().isEmpty()) {
            Color color = findOrCreateColor(request.getColorName().trim(), request.getColorHex());
            variantRequest.setColorId(color.getId());
        }

        // Set inventory
        InventoryRequest inventoryRequest = new InventoryRequest();
        inventoryRequest.setQuantityOnHand(request.getQuantityOnHand());
        inventoryRequest.setQuantityReserved(request.getQuantityReserved());
        inventoryRequest.setReorderLevel(request.getReorderLevel());
        variantRequest.setInventory(inventoryRequest);

        // Set image
        if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            ProductImageRequest imageRequest = new ProductImageRequest();
            imageRequest.setImageUrl(request.getImageUrl());
            imageRequest.setAlt(request.getImageAlt());
            imageRequest.setPosition(request.getImagePosition());
            imageRequest.setDefault(request.getIsDefaultImage());
            variantRequest.setImage(imageRequest);
        }

        return productVariantService.addVariant(variantRequest, productId).getId();
    }

    private ProductCreateRequest buildProductCreateRequest(ProductBulkImportRequest request, Brand brand, List<Category> categories) {
        ProductCreateRequest productRequest = new ProductCreateRequest();
        productRequest.setName(request.getName());
        productRequest.setSlug(request.getSlug());
        productRequest.setDescription(request.getDescription());
        productRequest.setMaterial(request.getMaterial());
        productRequest.setGender(request.getGender());
        productRequest.setPriceAmount(request.getPriceAmount());
        productRequest.setSeoTitle(request.getSeoTitle());
        productRequest.setSeoDescription(request.getSeoDescription());

        if (brand != null) {
            productRequest.setBrandId(brand.getId());
        }

        if (categories != null && !categories.isEmpty()) {
            productRequest.setCategoryId(categories.stream()
                    .map(Category::getId)
                    .collect(Collectors.toList()));
        }

        return productRequest;
    }

    private Brand findOrCreateBrand(String brandName) {
        if (brandName == null || brandName.trim().isEmpty()) {
            return null;
        }

        String normalizedName = brandName.trim();
        return brandRepository.findByName(normalizedName)
                .orElseGet(() -> {
                    Brand newBrand = Brand.builder()
                            .name(normalizedName)
                            .slug(generateSlug(normalizedName))
                            .build();
                    return brandRepository.save(newBrand);
                });
    }

    private List<Category> findOrCreateCategories(List<String> categoryNames) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            return new ArrayList<>();
        }

        return categoryNames.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(name -> {
                    String normalizedName = name.trim();
                    return categoryRepository.findByName(normalizedName)
                            .orElseGet(() -> {
                                Category newCategory = Category.builder()
                                        .name(normalizedName)
                                        .slug(generateSlug(normalizedName))
                                        .build();
                                return categoryRepository.save(newCategory);
                            });
                })
                .collect(Collectors.toList());
    }

    private Size findOrCreateSize(String sizeName) {
        return sizeRepository.findByName(sizeName)
                .orElseGet(() -> {
                    Size newSize = Size.builder()
                            .name(sizeName)
                            .code(sizeName) // Set code same as name
                            .build();
                    return sizeRepository.save(newSize);
                });
    }

    private Color findOrCreateColor(String colorName, String colorHex) {
        return colorRepository.findByName(colorName)
                .orElseGet(() -> {
                    Color newColor = Color.builder()
                            .name(colorName)
                            .hexCode(colorHex != null ? colorHex : "#000000")
                            .build();
                    return colorRepository.save(newColor);
                });
    }

    // Utility methods
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < 25; i++) { // Check first 25 columns
            Cell cell = row.getCell(i);
            if (cell != null && !getCellValueAsString(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private Long getCellValueAsLong(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (long) cell.getNumericCellValue();
            case STRING:
                try {
                    return Long.parseLong(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private Boolean getCellValueAsBoolean(Cell cell) {
        if (cell == null) return false;

        switch (cell.getCellType()) {
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case STRING:
                String value = cell.getStringCellValue().trim().toLowerCase();
                return "true".equals(value) || "1".equals(value) || "yes".equals(value);
            case NUMERIC:
                return cell.getNumericCellValue() == 1.0;
            default:
                return false;
        }
    }
}
