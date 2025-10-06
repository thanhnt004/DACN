package com.example.backend.service.product;

import com.example.backend.dto.request.product.InventoryAdjustRequest;
import com.example.backend.dto.request.product.InventoryReserveRequest;
import com.example.backend.dto.request.product.VariantCreateRequest;
import com.example.backend.dto.request.product.VariantUpdateRequest;
import com.example.backend.dto.response.product.VariantResponse;
import com.example.backend.excepton.BadRequestException;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.ProductVariantMapper;
import com.example.backend.model.product.Inventory;
import com.example.backend.model.product.Product;
import com.example.backend.model.product.ProductVariant;
import com.example.backend.model.product.VariantStatus;
import com.example.backend.repository.product.ProductRepository;
import com.example.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductVariantService {
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper mapper;
    public void deleteVariant(UUID variantId,UUID productId)
    {
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new NotFoundException("Variant not found!"));
        Product product = productRepository.findById(productId).orElseThrow(()->new NotFoundException("Product not found!"));
        product.removeVariant(productVariant);
        productRepository.save(product);
        productVariantRepository.delete(productVariant);
    }
    @Transactional
    public VariantResponse addVariant(VariantCreateRequest request,UUID productId)
    {
        validateSku(request.getSku());
        validateExist(request.getColorId(),request.getSizeId());
        //get product
        Product product = productRepository.findById(productId).orElseThrow(()->new NotFoundException("Product not found!"));
        ProductVariant newVariant = mapper.toEntity(request);
        product.addVariant(newVariant);
        newVariant = productVariantRepository.save(newVariant);
        productRepository.save(product);
        return mapper.toResponse(newVariant);
    }


    public void changeStatus(VariantUpdateRequest request, UUID id){
        if (request.getStatus().isBlank())
            throw new BadRequestException("Please provide new status. ");
        productVariantRepository.updateStatus(id, VariantStatus.valueOf(request.getStatus()),request.getVersion());
    }
    @Transactional
    public void changeQuantityOnHand(UUID id, InventoryAdjustRequest request){
        ProductVariant productVariant = productVariantRepository.findById(id).orElseThrow();
        Inventory inventory = productVariant.getInventory();
        //validate value
        if(inventory.getQuantityOnHand() + request.getAdjustment()<0)
            throw new BadRequestException("Quantity on hand cannot negative");
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + request.getAdjustment());
    }
    @Transactional
    public void reserveChange(UUID id, int quantity){
        ProductVariant productVariant = productVariantRepository.findById(id).orElseThrow();
        Inventory inventory = productVariant.getInventory();
        int newValue = inventory.getQuantityReserved() + quantity;
        if (quantity>0&&(inventory.getQuantityOnHand() - newValue<0))
            throw new BadRequestException("Quantity on hand cannot negative");
    }
    private void validateSku(String sku)
    {
        if (productVariantRepository.existsBySkuIgnoreCase(sku))
            throw new BadRequestException("Sku is existed!");
    }
    private void validateExist(UUID colorId, UUID sizeId) {
        if (productVariantRepository.existsByColor_IdAndSize_Id(colorId,sizeId))
            throw new BadRequestException("Variant is existed!");
    }

}
