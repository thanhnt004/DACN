package com.example.backend.controller.catalog.product;

import com.example.backend.dto.response.catalog.product.ProductBulkImportResponse;
import com.example.backend.exception.BadRequestException;
import com.example.backend.service.product.ExcelTemplateService;
import com.example.backend.service.product.ProductBulkImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class ProductBulkController {

    private final ProductBulkImportService productBulkImportService;
    private final ExcelTemplateService excelTemplateService;

    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductBulkImportResponse> bulkImport(
            @RequestParam("file") MultipartFile file) {

        // Validate file
        if (file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null ||
                (!originalFilename.toLowerCase().endsWith(".xlsx") &&
                        !originalFilename.toLowerCase().endsWith(".xls"))) {
            throw new BadRequestException("File phải là định dạng Excel (.xlsx hoặc .xls)");
        }

        try {
            ProductBulkImportResponse response = productBulkImportService.importFromExcel(file);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing Excel file: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi xử lý file Excel: " + e.getMessage());
        }
    }

    @GetMapping("/bulk-import/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] templateData = excelTemplateService.generateProductImportTemplate();

            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("product_import_template_%s.xlsx", timestamp);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(templateData.length);

            log.info("Generated product import template: {}", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(templateData);

        } catch (Exception e) {
            log.error("Error generating Excel template: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi tạo file template: " + e.getMessage());
        }
    }

    @GetMapping("/bulk-import/sample")
    public ResponseEntity<byte[]> downloadSampleFile() {
        // This endpoint returns the same template but with a different name
        // indicating it contains sample data
        try {
            byte[] templateData = excelTemplateService.generateProductImportTemplate();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("product_import_sample_%s.xlsx", timestamp);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(templateData.length);

            log.info("Generated product import sample file: {}", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(templateData);

        } catch (Exception e) {
            log.error("Error generating sample Excel file: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi tạo file mẫu: " + e.getMessage());
        }
    }

}
