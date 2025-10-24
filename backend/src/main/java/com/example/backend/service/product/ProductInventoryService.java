package com.example.backend.service.product;

import com.example.backend.dto.request.catalog.product.InventoryRequest;
import com.example.backend.dto.response.catalog.product.InventoryResponse;
import com.example.backend.excepton.ConflictException;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.InventoryMapper;
import com.example.backend.model.product.Inventory;
import com.example.backend.model.product.ProductVariant;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductInventoryService {
    private final ProductVariantRepository productVariantRepository;
    private final InventoryMapper inventoryMapper;
    public InventoryResponse create(UUID variantId,InventoryRequest request)
    {
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new NotFoundException("Variant not found!"));
        if (productVariant.getInventory()!=null)
            throw new ConflictException("Variant has an existed inventory!");
        Inventory inventory = inventoryMapper.toEntity(request);
        productVariant.setInventory(inventory);
        productVariantRepository.save(productVariant);
        return inventoryMapper.toDto(inventory);
    }

    public InventoryResponse update(UUID variantId, InventoryRequest request) {
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new NotFoundException("Variant not found!"));
        Inventory inventory = productVariant.getInventory();
        if (inventory==null)
            return create(variantId,request);
        inventoryMapper.updateFromDto(inventory,request);
        productVariant.setInventory(inventory);
        productVariantRepository.save(productVariant);
        return inventoryMapper.toDto(inventory);
    }

    public InventoryResponse get(UUID variantId) {
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new NotFoundException("Variant not found!"));
        Inventory inventory = productVariant.getInventory();
        return inventoryMapper.toDto(inventory);
    }
}
