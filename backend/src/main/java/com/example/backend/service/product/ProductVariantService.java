package com.example.backend.service.product;

import com.example.backend.dto.request.catalog.product.VariantCreateRequest;
import com.example.backend.dto.request.catalog.product.VariantUpdateRequest;
import com.example.backend.dto.response.catalog.product.VariantResponse;
import com.example.backend.excepton.BadRequestException;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.InventoryMapper;
import com.example.backend.mapper.ProductVariantMapper;
import com.example.backend.model.product.Inventory;
import com.example.backend.model.product.Product;
import com.example.backend.model.product.ProductVariant;
import com.example.backend.model.product.VariantStatus;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductVariantService {
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper mapper;
    private final InventoryMapper inventoryMapper;
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
        newVariant.setInventory(inventoryMapper.toEntity(request.getInventoryRequest()));
        newVariant = productVariantRepository.save(newVariant);
        productRepository.save(product);
        return mapper.toResponse(newVariant);
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

    public VariantResponse update(UUID productId, UUID variantId, VariantUpdateRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(()->new NotFoundException("Product not found!"));
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new NotFoundException("Product variant not found!"));
        mapper.updateFromDto(productVariant,request);
        productVariant = productVariantRepository.save(productVariant);
        return mapper.toResponse(productVariant);
    }

    public List<VariantResponse> getList(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found!"));

        List<ProductVariant> variants = product.getVariants();
        if (variants == null || variants.isEmpty()) {
            return Collections.emptyList();
        }

        List<VariantResponse> responses = mapper.toResponse(variants);

        for (int i = 0; i < variants.size(); i++) {
            ProductVariant variant = variants.get(i);

            Inventory inv = variant.getInventory();

            responses.get(i).setInventory(
                    inventoryMapper.toDto(inv)
            );
        }

        return responses;
    }

    public VariantResponse getById(UUID productId, UUID variantId) {
        Product product = productRepository.findById(productId).orElseThrow(()->new NotFoundException("Product not found!"));
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new NotFoundException("Product variant not found!"));

        VariantResponse response =  mapper.toResponse(productVariant);
        response.setInventory(inventoryMapper.toDto(productVariant.getInventory()));
        return response;
    }
}
