package com.example.backend.crons;

import com.example.backend.model.product.Product;
import com.example.backend.repository.ProductEmbeddingRepository;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.service.ProductEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cron job tự động tạo embedding cho các sản phẩm chưa có dữ liệu embedding
 * Chạy mỗi 1 giờ để kiểm tra và xử lý
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEmbeddingCron {

    private final ProductRepository productRepository;
    private final ProductEmbeddingRepository embeddingRepository;
    private final ProductEmbeddingService embeddingService;

    @org.springframework.beans.factory.annotation.Value("${cron.embedding.enabled:true}")
    private boolean cronEnabled;

    @org.springframework.beans.factory.annotation.Value("${cron.embedding.batch-size:50}")
    private int batchSize;

    @org.springframework.beans.factory.annotation.Value("${cron.embedding.request-delay:500}")
    private long requestDelay;

    /**
     * Chạy mỗi 1 giờ để tạo embedding cho các sản phẩm mới
     * Cron expression: 0 0 * * * * = Mỗi giờ đúng phút 0
     */
    @Scheduled(cron = "${cron.embedding.generate-missing:0 0 * * * *}")
    @Transactional(readOnly = true)
    public void generateMissingEmbeddings() {
        if (!cronEnabled) {
            log.debug("Product embedding cron job is disabled");
            return;
        }

        log.info("Starting product embedding generation cron job");

        try {
            // Lấy tất cả product IDs đã có embedding
            List<UUID> productsWithEmbedding = embeddingRepository.findAllProductIds();

            log.info("Found {} products with existing embeddings", productsWithEmbedding.size());

            // Lấy các sản phẩm chưa có embedding
            List<Product> productsWithoutEmbedding = productRepository.findAll()
                    .stream()
                    .filter(p -> !productsWithEmbedding.contains(p.getId()))
                    .limit(batchSize)
                    .collect(Collectors.toList());

            if (productsWithoutEmbedding.isEmpty()) {
                log.info("No products without embeddings found");
                return;
            }

            log.info("Found {} products without embeddings. Starting generation...",
                    productsWithoutEmbedding.size());

            int successCount = 0;
            int failCount = 0;

            for (Product product : productsWithoutEmbedding) {
                try {
                    String content = createProductContent(product);
                    UUID productId = product.getId();

                    // Tạo embedding bất đồng bộ
                    embeddingService.generateProductEmbedding(productId, content);
                    successCount++;

                    log.debug("Queued embedding generation for product: {} (ID: {})",
                            product.getName(), productId);

                    // Thêm delay để tránh quá tải API
                    Thread.sleep(requestDelay);

                } catch (Exception e) {
                    failCount++;
                    log.error("Failed to generate embedding for product: {} (ID: {}). Error: {}",
                            product.getName(), product.getId(), e.getMessage());
                }
            }

            log.info("Product embedding generation cron job completed. Success: {}, Failed: {}",
                    successCount, failCount);

        } catch (Exception e) {
            log.error("Error in product embedding generation cron job: {}", e.getMessage(), e);
        }
    }

    /**
     * Chạy mỗi ngày lúc 2 giờ sáng để regenerate embedding cho các sản phẩm đã cập nhật
     * Cron expression: 0 0 2 * * * = Mỗi ngày lúc 2:00 AM
     */
    @Scheduled(cron = "${cron.embedding.regenerate-outdated:0 0 2 * * *}")
    @Transactional(readOnly = true)
    public void regenerateOutdatedEmbeddings() {
        if (!cronEnabled) {
            log.debug("Product embedding regeneration cron job is disabled");
            return;
        }

        log.info("Starting outdated product embedding regeneration cron job");

        try {
            // Lấy các sản phẩm đã được cập nhật sau khi embedding được tạo
            List<Product> outdatedProducts = productRepository.findAll()
                    .stream()
                    .filter(product -> {
                        UUID productId = product.getId();
                        return embeddingRepository.findByProductId(productId)
                                .map(embedding -> product.getUpdatedAt().isAfter(
                                        embedding.getUpdatedAt().atZone(
                                                java.time.ZoneId.systemDefault()).toInstant()))
                                .orElse(false);
                    })
                    .limit(30) // Giới hạn 30 sản phẩm mỗi lần
                    .collect(Collectors.toList());

            if (outdatedProducts.isEmpty()) {
                log.info("No outdated product embeddings found");
                return;
            }

            log.info("Found {} products with outdated embeddings. Starting regeneration...",
                    outdatedProducts.size());

            int successCount = 0;
            int failCount = 0;

            for (Product product : outdatedProducts) {
                try {
                    String content = createProductContent(product);
                    UUID productId = product.getId();

                    embeddingService.generateProductEmbedding(productId, content);
                    successCount++;

                    log.debug("Queued embedding regeneration for product: {} (ID: {})",
                            product.getName(), productId);

                    Thread.sleep(requestDelay);

                } catch (Exception e) {
                    failCount++;
                    log.error("Failed to regenerate embedding for product: {} (ID: {}). Error: {}",
                            product.getName(), product.getId(), e.getMessage());
                }
            }

            log.info("Outdated embedding regeneration completed. Success: {}, Failed: {}",
                    successCount, failCount);

        } catch (Exception e) {
            log.error("Error in outdated embedding regeneration cron job: {}", e.getMessage(), e);
        }
    }

    /**
     * Tạo nội dung văn bản từ thông tin sản phẩm để embedding
     */
    private String createProductContent(Product product) {
        StringBuilder content = new StringBuilder();

        content.append("Tên sản phẩm: ").append(product.getName()).append(". ");

        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            content.append("Mô tả: ").append(product.getDescription()).append(". ");
        }

        if (product.getCategories() != null && !product.getCategories().isEmpty()) {
            String categories = product.getCategories().stream()
                    .map(c -> c.getName())
                    .collect(Collectors.joining(", "));
            content.append("Danh mục: ").append(categories).append(". ");
        }

        if (product.getBrand() != null) {
            content.append("Thương hiệu: ").append(product.getBrand().getName()).append(". ");
        }

        content.append("Giá: ").append(product.getPriceAmount()).append(" VNĐ. ");

        if (product.getMaterial() != null && !product.getMaterial().isBlank()) {
            content.append("Chất liệu: ").append(product.getMaterial()).append(". ");
        }

        if (product.getGender() != null) {
            content.append("Giới tính: ").append(product.getGender()).append(". ");
        }

        if (product.getSeoDescription() != null && !product.getSeoDescription().isBlank()) {
            content.append(product.getSeoDescription());
        }

        return content.toString();
    }
}

