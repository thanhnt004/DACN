package com.example.backend.service.product;

import com.example.backend.dto.request.catalog.product.ProductImageRequest;
import com.example.backend.dto.response.catalog.product.ProductImageResponse;
import com.example.backend.exception.NotFoundException;
import com.example.backend.mapper.ImageMapper;
import com.example.backend.model.product.Product;
import com.example.backend.model.product.ProductImage;
import com.example.backend.repository.catalog.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageService {
    private final ProductRepository productRepository;
    private final ImageMapper imageMapper;

    @Caching(evict = {
            @CacheEvict(value = "#{@cacheConfig.productCache}", allEntries = true),
            @CacheEvict(value = "#{@cacheConfig.productListCache}", allEntries = true)
    })
    public void addImage(UUID productId, ProductImageRequest imageDto)
    {
        Product product = productRepository.findById(productId).orElseThrow(
                ()->new NotFoundException("Product not found!")
        );
        product.addImage(imageMapper.toEntity(imageDto));
    }

    @Caching(evict = {
            @CacheEvict(value = "#{@cacheConfig.productCache}", allEntries = true),
            @CacheEvict(value = "#{@cacheConfig.productListCache}", allEntries = true)
    })
    public void removeImage(UUID productId, UUID imageId)  {
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new NotFoundException("Product not found!")
        );
        List<ProductImage> productImages = product.getImages();
        ProductImage toRemove = productImages.stream().filter(img->img.getId().equals(imageId)).toList().getFirst();
        product.removeImage(toRemove);
    }

    public List<ProductImageResponse> getImages(UUID productId) {
        Product product = productRepository.findById(productId).orElseThrow(
                ()->new NotFoundException("Product not found!")
        );
        List<ProductImage> productImages = product.getImages();
        return imageMapper.toDto(productImages);
    }

    @Caching(evict = {
            @CacheEvict(value = "#{@cacheConfig.productCache}", allEntries = true),
            @CacheEvict(value = "#{@cacheConfig.productListCache}", allEntries = true)
    })
    public void updateImage(UUID productId,UUID imageId, ProductImageRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(
                ()->new NotFoundException("Product not found!")
        );
        List<ProductImage> productImages = product.getImages();

        ProductImage productImage = productImages.stream().filter(img->{
            return img.getId().equals(imageId);
        }).toList().getFirst();
        if(productImage == null)
            throw new NotFoundException("Image Not Found!");
        imageMapper.updateFromDto(productImage,request);
        productRepository.save(product);
    }
}
