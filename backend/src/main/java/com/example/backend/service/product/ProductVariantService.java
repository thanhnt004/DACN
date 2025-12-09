package com.example.backend.service.product;

import com.example.backend.dto.request.catalog.product.VariantCreateRequest;
import com.example.backend.dto.request.catalog.product.VariantUpdateRequest;
import com.example.backend.dto.request.checkout.CheckOutItem;
import com.example.backend.dto.response.catalog.product.InventoryResponse;
import com.example.backend.dto.response.catalog.product.VariantResponse;
import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.product.ProductNotFoundException;
import com.example.backend.exception.product.VariantNotFoundException;
import com.example.backend.mapper.ImageMapper;
import com.example.backend.mapper.InventoryMapper;
import com.example.backend.mapper.ProductVariantMapper;
import com.example.backend.model.product.*;
import com.example.backend.repository.catalog.product.ColorRepository;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import com.example.backend.repository.catalog.product.SizeRepository;
import com.example.backend.service.audit.AuditLogService;
import com.example.backend.model.enumrator.AuditActionType;
import com.example.backend.model.enumrator.AuditEntityType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductVariantService {
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final ProductVariantMapper mapper;
    private final InventoryMapper inventoryMapper;
    private final ImageMapper imageMapper;
    private final AuditLogService auditLogService;
    @Caching(evict = {
        @CacheEvict(value = "#{@cacheConfig.productVariantsCache}", key = "#productId"),
        @CacheEvict(value = "#{@cacheConfig.productCache}", allEntries = true)
    })
    public void deleteVariant(UUID variantId,UUID productId)
    {
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new VariantNotFoundException("Không tìm thấy phiên bản sản phẩm"));
        Product product = productRepository.findById(productId).orElseThrow(()->new ProductNotFoundException("Không tìm thấy sản phẩm"));
        product.removeVariant(productVariant);
        
        if (product.getVariants().isEmpty()) {
            product.setStatus(ProductStatus.ARCHIVED);
        }
        
        productRepository.save(product);
        productVariantRepository.delete(productVariant);
    }
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "#{@cacheConfig.productVariantsCache}", key = "#productId"),
        @CacheEvict(value = "#{@cacheConfig.productCache}", allEntries = true)
    })
    public VariantResponse addVariant(VariantCreateRequest request,UUID productId)
    {
        validateSku(request.getSku());
        validateExist(request.getColorId(),request.getSizeId(),productId);

        //get product
        Product product = productRepository.findById(productId).orElseThrow(()->new ProductNotFoundException("Không tìm thấy sản phẩm"));
        
        // Update product price from new variant
        if (request.getPriceAmount() != null) {
            product.setPriceAmount(request.getPriceAmount());
        }

        Size size = null;
        if (request.getSizeId() != null) {
            size = sizeRepository.findById(request.getSizeId())
                    .orElseThrow(() -> new VariantNotFoundException("Không tìm thấy kích thước"));
        }

        Color color = null;
        if (request.getColorId() != null) {
            color = colorRepository.findById(request.getColorId())
                    .orElseThrow(() -> new VariantNotFoundException("Không tìm thấy màu sắc"));
        }
        ProductVariant newVariant = mapper.toEntity(request);
        newVariant.setSize(size);
        newVariant.setColor(color);
        product.addVariant(newVariant);

        // Set inventory
        if (request.getInventory() != null) {
            newVariant.setInventory(inventoryMapper.toEntity(request.getInventory()));
        }

        // Set image
        if (request.getImage() != null) {
            log.info("Setting image for variant {}", request.getSku());
            ProductImage variantImage = imageMapper.toEntity(request.getImage());
            variantImage.setProduct(product); 
            newVariant.setImage(variantImage);
        }

        newVariant = productVariantRepository.save(newVariant);
        productRepository.save(product);

        // Build response
        InventoryResponse inventoryResponse = inventoryMapper.toDto(newVariant.getInventory());
        VariantResponse response = mapper.toResponse(newVariant);
        response.setInventory(inventoryResponse);

        // Set image response
        if (newVariant.getImage() != null) {
            response.setImage(imageMapper.toDto(newVariant.getImage()));
        }

        return response;
    }
    public List<VariantResponse> addVariants(List<VariantCreateRequest> request,UUID productId)
    {
      return  request.stream().map(request1 -> addVariant(request1, productId)).toList();
    }
    private void validateSku(String sku)
    {
        if (productVariantRepository.existsBySkuIgnoreCase(sku))
            throw new BadRequestException("Sku is existed!");
    }
    private void validateExist(UUID colorId, UUID sizeId,UUID productId) {
        if (productVariantRepository.existsByColor_IdAndSize_IdAndProductId(colorId,sizeId,productId))
            throw new BadRequestException("Variant is existed!");
    }

    @Caching(evict = {
        @CacheEvict(value = "#{@cacheConfig.productVariantsCache}", key = "#productId"),
        @CacheEvict(value = "#{@cacheConfig.productCache}", allEntries = true)
    })
    public VariantResponse update(UUID productId, UUID variantId, VariantUpdateRequest request) {
        log.info("Update variant request: productId={}, variantId={}, request={}", productId, variantId, request);
        productRepository.findById(productId).orElseThrow(()->new ProductNotFoundException("Không tìm thấy sản phẩm"));
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new VariantNotFoundException("Không tìm thấy phiên bản sản phẩm"));

        // Lưu giá cũ để audit log
        Long oldPriceAmount = productVariant.getPriceAmount();
        Long oldCompareAtAmount = productVariant.getCompareAtAmount();
        
        log.info("Before update - SKU: {}, Price: {}, Status: {}", productVariant.getSku(), productVariant.getPriceAmount(), productVariant.getStatus());

        mapper.updateFromDto(productVariant,request);
        
        log.info("After mapper - SKU: {}, Price: {}, Status: {}", productVariant.getSku(), productVariant.getPriceAmount(), productVariant.getStatus());

        // Audit log: Thay đổi giá sản phẩm
        if (request.getPriceAmount() != null && !request.getPriceAmount().equals(oldPriceAmount)) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("product_id", productId.toString());
            metadata.put("variant_sku", productVariant.getSku());
            metadata.put("old_price", oldPriceAmount);
            metadata.put("new_price", request.getPriceAmount());

            auditLogService.logAction(
                AuditActionType.UPDATE_VARIANT_PRICE,
                AuditEntityType.PRODUCT_VARIANT,
                variantId,
                metadata
            );
        }

        // Audit log: Thay đổi giá so sánh
        if (request.getCompareAtAmount() != null && !request.getCompareAtAmount().equals(oldCompareAtAmount)) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("product_id", productId.toString());
            metadata.put("variant_sku", productVariant.getSku());
            metadata.put("old_compare_at_amount", oldCompareAtAmount);
            metadata.put("new_compare_at_amount", request.getCompareAtAmount());

            auditLogService.logAction(
                AuditActionType.UPDATE_VARIANT_PRICE,
                AuditEntityType.PRODUCT_VARIANT,
                variantId,
                metadata
            );
        }

        // Update size if provided
        if (request.getSizeId() != null) {
            Size size = sizeRepository.findById(request.getSizeId())
                    .orElseThrow(() -> new VariantNotFoundException("Không tìm thấy kích thước"));
            productVariant.setSize(size);
        }

        // Update color if provided
        if (request.getColorId() != null) {
            Color color = colorRepository.findById(request.getColorId())
                    .orElseThrow(() -> new VariantNotFoundException("Không tìm thấy màu sắc"));
            productVariant.setColor(color);
        }

        // Update inventory
        if (request.getInventory()!=null)
        {
            log.info("Updating inventory for variant {}", variantId);
            if (productVariant.getInventory()==null)
            {
                productVariant.setInventory(inventoryMapper.toEntity(request.getInventory()));
            }
            else
            {
                inventoryMapper.updateFromDto(productVariant.getInventory(),request.getInventory());
            }
        }

        // Update image
        if (request.getImage() != null) {
            log.info("Updating image for variant {}", variantId);
            if (productVariant.getImage() == null) {
                // Create new image
                ProductImage variantImage = imageMapper.toEntity(request.getImage());
                variantImage.setProduct(productVariant.getProduct()); // Set product for constraint
                productVariant.setImage(variantImage);
            } else {
                // Update existing image
                imageMapper.updateFromDto(productVariant.getImage(), request.getImage());
            }
        }

        productVariant = productVariantRepository.saveAndFlush(productVariant);
        log.info("Variant updated successfully: {}", productVariant.getId());

        // Build response
        VariantResponse response = mapper.toResponse(productVariant);

        // Set inventory response
        if (productVariant.getInventory() != null) {
            response.setInventory(inventoryMapper.toDto(productVariant.getInventory()));
        }

        // Set image response
        if (productVariant.getImage() != null) {
            response.setImage(imageMapper.toDto(productVariant.getImage()));
        }

        return response;
    }

    public List<VariantResponse> getList(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm"));

        List<ProductVariant> variants = product.getVariants();
        if (variants == null || variants.isEmpty()) {
            return Collections.emptyList();
        }

        List<VariantResponse> responses = mapper.toResponse(variants);

        for (int i = 0; i < variants.size(); i++) {
            ProductVariant variant = variants.get(i);

            // Set inventory
            Inventory inv = variant.getInventory();
            responses.get(i).setInventory(inventoryMapper.toDto(inv));

            // Set image
            if (variant.getImage() != null) {
                responses.get(i).setImage(imageMapper.toDto(variant.getImage()));
            }
        }

        return responses;
    }

    public VariantResponse getById(UUID productId, UUID variantId) {
        productRepository.findById(productId).orElseThrow(()->new ProductNotFoundException("Không tìm thấy sản phẩm"));
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new VariantNotFoundException("Không tìm thấy phiên bản sản phẩm"));

        VariantResponse response =  mapper.toResponse(productVariant);

        // Set inventory
        if (productVariant.getInventory() != null) {
            response.setInventory(inventoryMapper.toDto(productVariant.getInventory()));
        }

        // Set image
        if (productVariant.getImage() != null) {
            response.setImage(imageMapper.toDto(productVariant.getImage()));
        }

        return response;
    }

    public List<CheckoutItemDetail> getItemsForCheckout(@NotEmpty(message = "Danh sách sản phẩm không được để trống!") @Valid List<CheckOutItem> items) {
        return items.stream().map(item -> {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new VariantNotFoundException("Không tìm thấy phiên bản sản phẩm: " + item.getVariantId()));

            return CheckoutItemDetail.builder()
                    .variantId(variant.getId())
                    .productId(variant.getProduct().getId())
                    .productName(variant.getProduct().getName())
                    .variantName(buildVariantName(variant))
                    .sku(variant.getSku())
                    .imageUrl(variant.getImage() != null ? variant.getImage().getImageUrl() : variant.getProduct().getPrimaryImageUrl())
                    .weight(variant.getWeightGrams())
                    .unitPriceAmount(variant.getPriceAmount())
                    .compareAtAmount(variant.getCompareAtAmount())
                    .totalAmount(item.getQuantity()*variant.getPriceAmount())
                    .build();
        }).toList();
    }

    private String buildVariantName(ProductVariant variant) {
        if (variant == null) return null;

        StringBuilder sb = new StringBuilder();
        if (variant.getColor() != null && variant.getColor().getName() != null) {
            sb.append(variant.getColor().getName());
        }
        if (variant.getSize() != null && variant.getSize().getName() != null) {
            if (!sb.isEmpty()) sb.append(" / ");
            sb.append(variant.getSize().getName());
        }
        if (sb.isEmpty()) {
            return variant.getSku();
        }
        return sb.toString();
    }
}
