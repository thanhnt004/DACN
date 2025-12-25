
package com.example.backend.service.product;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class ExcelTemplateService {

    public byte[] generateProductImportTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Products Import Template");

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle requiredHeaderStyle = createRequiredHeaderStyle(workbook);
            CellStyle sampleDataStyle = createSampleDataStyle(workbook);
            CellStyle instructionStyle = createInstructionStyle(workbook);

            // Create headers
            createHeaders(sheet, headerStyle, requiredHeaderStyle);

            // Create sample data
            createSampleData(sheet, sampleDataStyle);

            // Create instructions sheet
            createInstructionsSheet(workbook, instructionStyle);

            // Auto-size columns
            autoSizeColumns(sheet);

            // Freeze header row
            sheet.createFreezePane(0, 1);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createHeaders(Sheet sheet, CellStyle headerStyle, CellStyle requiredHeaderStyle) {
        Row headerRow = sheet.createRow(0);

        String[] headers = {
                "*Product Name", "*Slug", "Description", "Material", "Gender",
                "Brand", "Categories", "SEO Title", "SEO Description",
                "*SKU", "Barcode", "Size", "Color", "Color Hex",
                "*Price (VND)", "Compare Price (VND)", "Cost (VND)", "Weight (grams)",
                "*Stock Quantity", "Reserved Quantity", "Reorder Level",
                "Image URL", "Image Alt", "Default Image", "Image Position"
        };

        String[] requiredFields = {
                "*Product Name", "*Slug", "*SKU", "*Price (VND)", "*Stock Quantity"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);

            if (isRequiredField(headers[i], requiredFields)) {
                cell.setCellStyle(requiredHeaderStyle);
            } else {
                cell.setCellStyle(headerStyle);
            }
        }
    }

    private void createSampleData(Sheet sheet, CellStyle sampleDataStyle) {
        // Sample data rows
        String[][] sampleData = {
                {
                        "Áo Thun Nam Nike Classic", "ao-thun-nam-nike-classic", "Áo thun cotton cao cấp, thoáng mát",
                        "Cotton 100%", "men", "Nike", "Áo, Nam, Thể thao", "Áo Thun Nam Nike Classic - Chất lượng cao",
                        "Áo thun nam Nike chất liệu cotton thoáng mát, phù hợp mọi hoạt động",
                        "NIKE-TSHIRT-001-S-BLACK", "1234567890123", "S", "Đen", "#000000",
                        "550000", "650000", "300000", "180", "100", "0", "10",
                        "https://example.com/nike-tshirt-black-s.jpg", "Áo thun Nike đen size S", "TRUE", "0"
                },
                {
                        "Áo Thun Nam Nike Classic", "ao-thun-nam-nike-classic", "Áo thun cotton cao cấp, thoáng mát",
                        "Cotton 100%", "men", "Nike", "Áo, Nam, Thể thao", "Áo Thun Nam Nike Classic - Chất lượng cao",
                        "Áo thun nam Nike chất liệu cotton thoáng mát, phù hợp mọi hoạt động",
                        "NIKE-TSHIRT-001-M-BLACK", "1234567890124", "M", "Đen", "#000000",
                        "550000", "650000", "300000", "200", "150", "0", "10",
                        "https://example.com/nike-tshirt-black-m.jpg", "Áo thun Nike đen size M", "FALSE", "1"
                },
                {
                        "Áo Thun Nam Nike Classic", "ao-thun-nam-nike-classic", "Áo thun cotton cao cấp, thoáng mát",
                        "Cotton 100%", "men", "Nike", "Áo, Nam, Thể thao", "Áo Thun Nam Nike Classic - Chất lượng cao",
                        "Áo thun nam Nike chất liệu cotton thoáng mát, phù hợp mọi hoạt động",
                        "NIKE-TSHIRT-001-L-WHITE", "1234567890125", "L", "Trắng", "#FFFFFF",
                        "550000", "650000", "300000", "220", "80", "0", "10",
                        "https://example.com/nike-tshirt-white-l.jpg", "Áo thun Nike trắng size L", "FALSE", "2"
                },
                {
                        "Giày Sneaker Adidas Ultra", "giay-sneaker-adidas-ultra", "Giày sneaker thể thao cao cấp",
                        "Synthetic + Mesh", "unisex", "Adidas", "Giày, Thể thao", "Giày Sneaker Adidas Ultra - Phong cách",
                        "Giày sneaker Adidas với công nghệ đệm tối ưu cho việc chạy bộ",
                        "ADIDAS-ULTRA-002-38-BLUE", "2345678901234", "38", "Xanh", "#0066CC",
                        "2500000", "3000000", "1500000", "500", "50", "0", "5",
                        "https://example.com/adidas-ultra-blue-38.jpg", "Giày Adidas Ultra xanh size 38", "TRUE", "0"
                },
                {
                        "Váy Maxi Nữ Zara", "vay-maxi-nu-zara", "Váy maxi sang trọng cho phái đẹp",
                        "Polyester + Spandex", "women", "Zara", "Váy, Nữ, Thời trang", "Váy Maxi Nữ Zara - Thanh lịch",
                        "Váy maxi Zara thiết kế thanh lịch, phù hợp dự tiệc và dạo phố",
                        "ZARA-MAXI-003-S-RED", "3456789012345", "S", "Đỏ", "#CC0000",
                        "1200000", "1500000", "800000", "300", "30", "0", "8",
                        "https://example.com/zara-maxi-red-s.jpg", "Váy Zara maxi đỏ size S", "TRUE", "0"
                }
        };

        for (int i = 0; i < sampleData.length; i++) {
            Row dataRow = sheet.createRow(i + 1);
            for (int j = 0; j < sampleData[i].length; j++) {
                Cell cell = dataRow.createCell(j);

                // Handle numeric fields
                if (isNumericColumn(j)) {
                    try {
                        if (j == 23 || j == 24) { // Boolean and position columns
                            if ("TRUE".equalsIgnoreCase(sampleData[i][j])) {
                                cell.setCellValue(true);
                            } else if ("FALSE".equalsIgnoreCase(sampleData[i][j])) {
                                cell.setCellValue(false);
                            } else {
                                cell.setCellValue(Double.parseDouble(sampleData[i][j]));
                            }
                        } else {
                            cell.setCellValue(Double.parseDouble(sampleData[i][j]));
                        }
                    } catch (NumberFormatException e) {
                        cell.setCellValue(sampleData[i][j]);
                    }
                } else {
                    cell.setCellValue(sampleData[i][j]);
                }

                cell.setCellStyle(sampleDataStyle);
            }
        }
    }

    private void createInstructionsSheet(Workbook workbook, CellStyle instructionStyle) {
        Sheet instructionsSheet = workbook.createSheet("Hướng dẫn");

        String[] instructions = {
                "HƯỚNG DẪN SỬ DỤNG TEMPLATE IMPORT SẢN PHẨM",
                "",
                "1. CÁC TRƯỜNG BẮT BUỘC (được đánh dấu * trong header):",
                "   - Product Name: Tên sản phẩm",
                "   - Slug: URL slug của sản phẩm (không dấu, viết liền)",
                "   - SKU: Mã sản phẩm duy nhất",
                "   - Price: Giá bán (đơn vị VND)",
                "   - Stock Quantity: Số lượng tồn kho",
                "",
                "2. CÁC TRƯỜNG TÙY CHỌN:",
                "   - Description: Mô tả sản phẩm",
                "   - Material: Chất liệu",
                "   - Gender: Giới tính (men/women/unisex)",
                "   - Brand: Tên thương hiệu",
                "   - Categories: Danh mục (phân cách bằng dấu phẩy)",
                "   - SEO Title/Description: Thông tin SEO",
                "   - Barcode: Mã vạch",
                "   - Size: Kích thước",
                "   - Color: Màu sắc",
                "   - Color Hex: Mã màu hex (ví dụ: #FF0000)",
                "   - Compare Price: Giá so sánh",
                "   - Cost: Giá vốn",
                "   - Weight: Cân nặng (gram)",
                "   - Reserved Quantity: Số lượng đã đặt trước",
                "   - Reorder Level: Mức tồn kho tối thiểu",
                "   - Image URL: Đường dẫn ảnh sản phẩm",
                "   - Image Alt: Mô tả ảnh",
                "   - Default Image: Ảnh mặc định (TRUE/FALSE)",
                "   - Image Position: Vị trí ảnh (số thứ tự)",
                "",
                "3. LƯU Ý QUAN TRỌNG:",
                "   - Các sản phẩm có cùng tên và slug sẽ được nhóm thành một sản phẩm với nhiều variant",
                "   - Mỗi variant phải có SKU khác nhau",
                "   - Brand, Category, Size, Color sẽ tự động tạo mới nếu chưa tồn tại",
                "   - Giá trị TRUE/FALSE không phân biệt hoa thường",
                "   - Categories có thể có nhiều giá trị, phân cách bằng dấu phẩy",
                "",
                "4. ĐỊNH DẠNG DỮ LIỆU:",
                "   - Giá tiền: Số nguyên (VND)",
                "   - Số lượng: Số nguyên dương",
                "   - Gender: men | women | unisex",
                "   - Default Image: TRUE | FALSE",
                "",
                "5. CÁCH SỬ DỤNG:",
                "   - Xóa các dòng dữ liệu mẫu",
                "   - Nhập dữ liệu sản phẩm của bạn",
                "   - Lưu file với định dạng .xlsx",
                "   - Upload file qua API import",
                "",
                "Chúc bạn import thành công!"
        };

        for (int i = 0; i < instructions.length; i++) {
            Row row = instructionsSheet.createRow(i);
            Cell cell = row.createCell(0);
            cell.setCellValue(instructions[i]);
            cell.setCellStyle(instructionStyle);
        }

        // Auto-size the instruction column
        instructionsSheet.autoSizeColumn(0);
        instructionsSheet.setColumnWidth(0, Math.min(instructionsSheet.getColumnWidth(0), 20000));
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 25; i++) {
            sheet.autoSizeColumn(i);
            // Set max width to prevent too wide columns
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth, 8000));
        }
    }

    private boolean isRequiredField(String header, String[] requiredFields) {
        for (String required : requiredFields) {
            if (required.equals(header)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNumericColumn(int columnIndex) {
        // Columns: Price, Compare Price, Cost, Weight, Stock, Reserved, Reorder, Default Image (boolean), Position
        return columnIndex == 14 || columnIndex == 15 || columnIndex == 16 || columnIndex == 17 ||
                columnIndex == 18 || columnIndex == 19 || columnIndex == 20 || columnIndex == 23 || columnIndex == 24;
    }

    // Style creation methods
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        return style;
    }

    private CellStyle createRequiredHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        return style;
    }

    private CellStyle createSampleDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    private CellStyle createInstructionStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);

        return style;
    }
}