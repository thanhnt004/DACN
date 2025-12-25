package com.example.backend.service.product;

import com.example.backend.config.CacheConfig;
import com.example.backend.controller.catalog.product.Options;
import com.example.backend.controller.catalog.product.ProductFilter;
import com.example.backend.dto.request.catalog.product.ProductCreateRequest;
import com.example.backend.dto.request.catalog.product.ProductImageRequest;
import com.example.backend.dto.request.catalog.product.ProductUpdateRequest;
import com.example.backend.dto.response.catalog.ColorDto;
import com.example.backend.dto.response.catalog.SizeDto;
import com.example.backend.dto.response.catalog.product.ProductDetailResponse;
import com.example.backend.dto.response.catalog.product.ProductSummaryResponse;
import com.example.backend.dto.response.catalog.product.VariantResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.product.DuplicateProductException;
import com.example.backend.exception.product.ProductNotFoundException;
import com.example.backend.mapper.*;
import com.example.backend.model.product.Brand;
import com.example.backend.model.product.Category;
import com.example.backend.model.product.Product;
import com.example.backend.model.product.ProductImage;
import com.example.backend.model.product.ProductStatus;
import com.example.backend.model.product.ProductVariant;
import com.example.backend.repository.catalog.brand.BrandRepository;
import com.example.backend.repository.catalog.categoty.CategoryRepository;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.repository.catalog.product.ProductSpecification;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import com.example.backend.service.ProductEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandMapper brandMapper;
    private final BrandRepository brandRepository;

    private final ProductMapper productMapper;
    private final ImageMapper imageMapper;
    private final ProductVariantMapper variantsMapper;
    private final CloudImageService imageService;
    private final CategoryMapper categoryMapper;

    private final CacheConfig cacheConfig;
    private static final Set<String> ALLOWED_INCLUDES = Set.of("variants", "images", "categories", "options");

    private final ProductEmbeddingService embeddingService;

    private boolean slugExist(String slug, UUID excludedId) {
        if (excludedId == null) {
            return productRepository.existsBySlugIncludeDeleted(slug);
        }
        return productRepository.existsBySlugAndIdNotIncludeDeleted(slug, excludedId);
    }
    // 3 query: product, colors, sizes
    @Cacheable(
        value = "#{@cacheConfig.productListCache}",
        key = "#filter.toString() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
        unless = "#result == null"
    )
    public PageResponse<ProductSummaryResponse> list(ProductFilter filter, Pageable pageable) {
        // Build pageable with sort from filter
        Pageable effectivePageable = buildPageable(filter, pageable);
        //find products
        Page<Product> page = productRepository.findAll(getFilterSpecification(filter), effectivePageable);
        if (page.isEmpty()) {
            return null;
        }
        //get ids
        List<UUID> productIds = page.getContent().stream().map(Product::getId).toList();
        //find colors
        Map<UUID, List<String>> colorsMap = new HashMap<>();
        List<Object[]> results = variantRepository.findColorsByProductIds(productIds);
        for (Object[] result : results) {
            UUID productId = (UUID) result[0];
            ColorDto colorDto = (ColorDto) result[1];
            colorsMap.computeIfAbsent(productId, k -> new ArrayList<>()).add(colorDto.getHexCode());
        }
        //find sizes
        Map<UUID, List<String>> sizeMap = new HashMap<>();
        List<Object[]> sizeResults = variantRepository.findSizesByProductIds(productIds);
        for (Object[] result : sizeResults) {
            UUID productId = (UUID) result[0];
            SizeDto sizeDto = (SizeDto) result[1];
            sizeMap.computeIfAbsent(productId, k -> new ArrayList<>()).add(sizeDto.getCode());
        }
        return new PageResponse<>(page.map(p -> toSummaryResponse(p, colorsMap.getOrDefault(p.getId(), List.of()),sizeMap.getOrDefault(p.getId(), List.of()))));
    }

    @Caching(evict = {
        @CacheEvict(value = "#{@cacheConfig.productListCache}", allEntries = true)
    })
    public ProductDetailResponse create(ProductCreateRequest productCreateRequest) {
        Product product = productMapper.toEntity(productCreateRequest);
        if (slugExist(productCreateRequest.getSlug(), null)) {
            throw new DuplicateProductException("SLUG", "Slug sản phẩm đã tồn tại");
        }
        product.setSlug(productCreateRequest.getSlug());
        if (product.getSeoTitle() == null || product.getSeoTitle().isBlank())
            product.setSeoTitle(product.getName());

        // Handle brand
        if (productCreateRequest.getBrandId() != null) {
            Brand brand = brandRepository.findById(productCreateRequest.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand không tồn tại với ID: " + productCreateRequest.getBrandId()));
            product.setBrand(brand);
        }

        // Handle categories
        if (productCreateRequest.getCategoryId() != null && !productCreateRequest.getCategoryId().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(productCreateRequest.getCategoryId());
            product.setCategories(categories);
        }

        if (productCreateRequest.getImages() != null) {
            for (ProductImageRequest imageDto : productCreateRequest.getImages()) {
                product.addImage(imageMapper.toEntity(imageDto));
            }
        }
        product = productRepository.save(product);
        return productMapper.toDto(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "#{@cacheConfig.productCache}", allEntries = true),
            @CacheEvict(value = "#{@cacheConfig.productVariantsCache}", key = "#id"),
            @CacheEvict(value = "#{@cacheConfig.productListCache}", allEntries = true)
    })
    public ProductDetailResponse update(ProductUpdateRequest request, UUID id) {
        Product exist = productRepository.findById(id).orElseThrow(
                () -> new ProductNotFoundException("Không tìm thấy sản phẩm"));
        if (StringUtils.hasText(request.getSlug())) {
            if (slugExist(request.getSlug(), id)) {
                throw new DuplicateProductException("SLUG", "Slug sản phẩm đã tồn tại");
            }
        }
        productMapper.updateFromDto(request, exist);

        // Handle brand
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand không tồn tại với ID: " + request.getBrandId()));
            exist.setBrand(brand);
        }

        // Handle categories
        if (request.getCategoryId() != null) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryId());
            exist.setCategories(categories);
        }

        if (request.getImages() != null) {
            List<ProductImage> images = request.getImages().stream().map(imageMapper::toEntity).toList();
            log.info("Received {} images for update", images.size());
            images.forEach(img -> {
                ProductVariant variant = null;
                UUID requestedVariantId = (img.getVariant() != null) ? img.getVariant().getId() : null;
                log.info("Processing image. Requested Variant ID: {}", requestedVariantId);
                if (requestedVariantId != null) {
                    variant = variantRepository.findById(requestedVariantId)
                            .orElse(null);
                    log.info("Found variant: {}", (variant != null ? variant.getId() : "null"));
                }
                img.setVariant(variant);
            });
            exist.syncImage(images);
        }
        exist = productRepository.save(exist);
        return productMapper.toDto(exist);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "#{@cacheConfig.productCache}", key = "#productId"),
            @CacheEvict(value = "#{@cacheConfig.productVariantsCache}", key = "#productId"),
            @CacheEvict(value = "#{@cacheConfig.productListCache}", allEntries = true)
    })
    public void delete(UUID productId) {
        Product exist = productRepository.findById(productId).orElseThrow(
                () -> new ProductNotFoundException("Không tìm thấy sản phẩm"));
        productRepository.delete(exist);
    }
    @Cacheable(value = "#{@cacheConfig.productVariantsCache}", key = "#product.id")
    public List<VariantResponse> getVariantsByProduct(Product product) {
        return variantsMapper.toResponse(product.getVariants());
    }
    @Cacheable(
        value = "#{@cacheConfig.productCache}",
        key = "#slugOrId + ':' + (#includes != null ? #includes.toString() : 'none')",
        unless = "#result == null"
    )
    public ProductDetailResponse findBySlugOrId(String slugOrId, List<String> includes) {
        List<String> unknown = includes.stream()
                .filter(s -> !ALLOWED_INCLUDES.contains(s))
                .toList();
        if (!unknown.isEmpty())
            throw new BadRequestException("Invalid include value: " + String.join(", ", unknown)
                    + ". Allow: " + String.join(", ", ALLOWED_INCLUDES));
        Product product;
        try {
            UUID id = UUID.fromString(slugOrId);
            product = productRepository.findById(id).orElseThrow(
                    () -> new ProductNotFoundException("Không tìm thấy sản phẩm"));
        } catch (Exception e) {
            product = productRepository.findBySlug(slugOrId).orElseThrow(
                    () -> new ProductNotFoundException("Không tìm thấy sản phẩm"));
        }
        Set<String> includeSet = normalizeIncludes(includes);
        ProductDetailResponse response = productMapper.toDto(product);

        // Populate brand using BrandMapper
        if (product.getBrand() != null) {
            response.setBrand(brandMapper.toDto(product.getBrand()));
        }

        if (includeSet.contains("variants")) {
            response.setVariants(variantsMapper.toResponse(product.getVariants()));
        }

        if (includeSet.contains("categories"))
            response.setCategories(categoryMapper.toDto(product.getCategories()));
        if (includeSet.contains("images"))
            response.setImages(imageMapper.toDto(product.getImages()));
        if (includeSet.contains("options")) {
            List<ColorDto> colorDto = variantRepository.findDistinctColorsByProductId(product.getId());
            List<SizeDto> sizeDto = variantRepository.getSizesByProductId(product.getId());
            Options options = Options.builder()
                    .color(colorDto)
                    .size(sizeDto)
                    .build();
            response.setOptions(options);
        }
        return response;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "#{@cacheConfig.productCache}", key = "#id"),
            @CacheEvict(value = "#{@cacheConfig.productListCache}", allEntries = true)
    })
    public void changeStatus(UUID id, ProductStatus status) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new ProductNotFoundException("Không tìm thấy sản phẩm"));
        if (product.getStatus().equals(status))
            return;
        if (status.equals(ProductStatus.ACTIVE))
            validateProduct(product);
        product.setStatus(status);
        productRepository.save(product);
    }

    private void validateProduct(Product product) {
        if (variantRepository.countByProductId(product.getId()) <= 0)
            throw new BadRequestException("Product must have at least one variant!");
    }

    private Set<String> normalizeIncludes(List<String> raw) {
        if (raw == null || raw.isEmpty())
            return Collections.emptySet();
        return raw.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Specification<Product> getFilterSpecification(ProductFilter filter) {
        // If search query exists, use vector search to filter products
        Set<UUID> searchProductIds = null;
        if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
            Map<UUID, Double> similarityMap = embeddingService.searchProductIdsBySimilarity(filter.getSearch());
            searchProductIds = similarityMap.keySet();
        }

        return Specification.allOf(
                ProductSpecification.hasStatus(filter.getStatus()),
                ProductSpecification.hasCategory(filter.getCategoryId()),
                ProductSpecification.hasBrand(filter.getBrandId()),
                ProductSpecification.hasMinPrice(filter.getMinPriceAmount()),
                ProductSpecification.hasMaxPrice(filter.getMaxPriceAmount()),
                ProductSpecification.hasIdIn(searchProductIds),
                ProductSpecification.hasVariantColors(filter.getColorIds()),
                ProductSpecification.hasVariantSizes(filter.getSizeIds()),
                ProductSpecification.fetchBrand()
        );
    }

    private Pageable buildPageable(ProductFilter filter, Pageable pageable) {
        Sort sortFromFilter = buildSortFromFilter(filter);

        int page = (pageable != null) ? pageable.getPageNumber() : 0;
        int size = (pageable != null) ? pageable.getPageSize() : 20;

        return PageRequest.of(page, size, sortFromFilter);
    }

    private Sort buildSortFromFilter(ProductFilter filter) {
        if (filter == null)
            return null;
        String sortBy = filter.getSortBy();
        if (sortBy == null || sortBy.isBlank())
            return Sort.by(new Sort.Order(Sort.Direction.DESC, "updatedAt"));

        String normalized = sortBy.trim();
        if (!ProductFilter.ALLOW_SORT_LIST.contains(normalized)) {
            return Sort.by(new Sort.Order(Sort.Direction.DESC, "updatedAt"));
        }
        Sort.Direction dir = parseDirection(filter.getDirection());
        return Sort.by(new Sort.Order(dir, filter.getSortBy()));
    }

    private Sort.Direction parseDirection(String direction) {
        if (direction == null)
            return Sort.Direction.DESC;
        String d = direction.trim().toUpperCase();
        if ("ASC".equals(d))
            return Sort.Direction.ASC;
        if ("DESC".equals(d))
            return Sort.Direction.DESC;
        return Sort.Direction.DESC;
    }

    private ProductSummaryResponse toSummaryResponse(Product p, List<String> colors, List<String> sizes) {
        ProductSummaryResponse r = new ProductSummaryResponse();
        r.setId(p.getId());
        r.setSlug(p.getSlug());
        r.setColors(colors);
        r.setImageUrl(p.getPrimaryImageUrl());
        r.setName(p.getName());
        r.setSizes(sizes);
        r.setStatus(p.getStatus());
        r.setGender(String.valueOf(p.getGender()));
        r.setPriceAmount(p.getPriceAmount());
        
        // Check if product has any variant in stock and calculate total available stock
        int totalStock = 0;
        boolean inStock = false;
        if (p.getVariants() != null) {
            for (var v : p.getVariants()) {
                if (v.getInventory() != null && v.getInventory().getAvailableStock() > 0) {
                    inStock = true;
                    totalStock += v.getInventory().getAvailableStock();
                }
            }
        }
        r.setIsInStock(inStock);
        r.setTotalStock(totalStock);
        
        if (p.getBrand() != null) {
            r.setBrandId(p.getBrand().getId());
            r.setBrandName(p.getBrand().getName());
        }
        return r;
    }
    public String normalizeKeyForCache(Object idOrSlug, List<String> includes) {
        Set<String> set = normalizeIncludes(includes);
        String inc = String.join(",", set);
        return idOrSlug + ":" + inc;
    }
}
