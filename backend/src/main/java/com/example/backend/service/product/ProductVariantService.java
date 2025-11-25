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
import com.example.backend.mapper.InventoryMapper;
import com.example.backend.mapper.ProductVariantMapper;
import com.example.backend.model.product.*;
import com.example.backend.repository.catalog.product.ColorRepository;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import com.example.backend.repository.catalog.product.SizeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
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
    public void deleteVariant(UUID variantId,UUID productId)
    {
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new VariantNotFoundException("Không tìm thấy phiên bản sản phẩm"));
        Product product = productRepository.findById(productId).orElseThrow(()->new ProductNotFoundException("Không tìm thấy sản phẩm"));
        product.removeVariant(productVariant);
        productRepository.save(product);
        productVariantRepository.delete(productVariant);
    }
    @Transactional
    public VariantResponse addVariant(VariantCreateRequest request,UUID productId)
    {
        validateSku(request.getSku());
        validateExist(request.getColorId(),request.getSizeId(),productId);

        //get product
        Product product = productRepository.findById(productId).orElseThrow(()->new ProductNotFoundException("Không tìm thấy sản phẩm"));
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
        if (request.getInventory() != null) {
            newVariant.setInventory(inventoryMapper.toEntity(request.getInventory()));
        }
        newVariant = productVariantRepository.save(newVariant);
        productRepository.save(product);
        InventoryResponse inventoryResponse = inventoryMapper.toDto(newVariant.getInventory());
        VariantResponse response = mapper.toResponse(newVariant);
        response.setInventory(inventoryResponse);
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

    public VariantResponse update(UUID productId, UUID variantId, VariantUpdateRequest request) {
        productRepository.findById(productId).orElseThrow(()->new ProductNotFoundException("Không tìm thấy sản phẩm"));
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new VariantNotFoundException("Không tìm thấy phiên bản sản phẩm"));
        mapper.updateFromDto(productVariant,request);
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
        productVariant = productVariantRepository.save(productVariant);
        return mapper.toResponse(productVariant);
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

            Inventory inv = variant.getInventory();

            responses.get(i).setInventory(
                    inventoryMapper.toDto(inv)
            );
        }

        return responses;
    }

    public VariantResponse getById(UUID productId, UUID variantId) {
        productRepository.findById(productId).orElseThrow(()->new ProductNotFoundException("Không tìm thấy sản phẩm"));
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()->new VariantNotFoundException("Không tìm thấy phiên bản sản phẩm"));

        VariantResponse response =  mapper.toResponse(productVariant);
        response.setInventory(inventoryMapper.toDto(productVariant.getInventory()));
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
                    .imageUrl(variant.getImage()!=null?variant.getImage().getImageUrl():null)
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
