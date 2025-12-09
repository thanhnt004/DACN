package com.example.backend.service.product;

import com.example.backend.dto.request.catalog.product.InventoryRequest;
import com.example.backend.dto.response.catalog.product.InventoryResponse;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.mapper.InventoryMapper;
import com.example.backend.model.enumrator.AuditActionType;
import com.example.backend.model.enumrator.AuditEntityType;
import com.example.backend.model.product.Inventory;
import com.example.backend.model.product.ProductVariant;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import com.example.backend.service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductInventoryService {
    private final ProductVariantRepository productVariantRepository;
    private final InventoryMapper inventoryMapper;
    private final AuditLogService auditLogService;

    @Caching(evict = {
            @CacheEvict(value = "#{@cacheConfig.productCache}", allEntries = true),
            @CacheEvict(value = "#{@cacheConfig.productVariantsCache}", allEntries = true),
            @CacheEvict(value = "#{@cacheConfig.productListCache}", allEntries = true)
    })
    public InventoryResponse create(UUID variantId,InventoryRequest request)
    {
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new NotFoundException("Variant not found!"));
        if (productVariant.getInventory()!=null)
            throw new ConflictException("Variant has an existed inventory!");
        Inventory inventory = inventoryMapper.toEntity(request);
        productVariant.setInventory(inventory);
        productVariantRepository.save(productVariant);

        // Audit log: Tạo inventory thủ công
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("variant_id", variantId.toString());
        metadata.put("variant_sku", productVariant.getSku());
        metadata.put("quantity_on_hand", request.getQuantityOnHand());
        metadata.put("action_type", "CREATE");

        auditLogService.logAction(
            AuditActionType.ADJUST_STOCK_MANUAL,
            AuditEntityType.INVENTORY,
            variantId,
            metadata
        );

        return inventoryMapper.toDto(inventory);
    }

    @Caching(evict = {
            @CacheEvict(value = "#{@cacheConfig.productCache}", allEntries = true),
            @CacheEvict(value = "#{@cacheConfig.productVariantsCache}", allEntries = true),
            @CacheEvict(value = "#{@cacheConfig.productListCache}", allEntries = true)
    })
    public InventoryResponse update(UUID variantId, InventoryRequest request) {
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new NotFoundException("Variant not found!"));
        Inventory inventory = productVariant.getInventory();
        if (inventory==null)
            return create(variantId,request);

        // Lưu số lượng cũ để audit
        Integer oldQuantity = inventory.getQuantityOnHand();

        inventoryMapper.updateFromDto(inventory,request);
        productVariant.setInventory(inventory);
        productVariantRepository.save(productVariant);

        // Audit log: Điều chỉnh tồn kho thủ công
        if (request.getQuantityOnHand() != null && !request.getQuantityOnHand().equals(oldQuantity)) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("variant_id", variantId.toString());
            metadata.put("variant_sku", productVariant.getSku());
            metadata.put("old_quantity", oldQuantity);
            metadata.put("new_quantity", request.getQuantityOnHand());
            metadata.put("difference", request.getQuantityOnHand() - oldQuantity);
            metadata.put("action_type", "UPDATE");

            auditLogService.logAction(
                AuditActionType.ADJUST_STOCK_MANUAL,
                AuditEntityType.INVENTORY,
                variantId,
                metadata
            );
        }

        return inventoryMapper.toDto(inventory);
    }

    public InventoryResponse get(UUID variantId) {
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new NotFoundException("Variant not found!"));
        Inventory inventory = productVariant.getInventory();
        return inventoryMapper.toDto(inventory);
    }
}
