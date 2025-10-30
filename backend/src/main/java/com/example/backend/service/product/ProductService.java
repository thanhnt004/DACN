package com.example.backend.service.product;

import com.example.backend.controller.catalog.product.Options;
import com.example.backend.controller.catalog.product.ProductFilter;
import com.example.backend.dto.request.catalog.product.ProductCreateRequest;
import com.example.backend.dto.request.catalog.product.ProductImageRequest;
import com.example.backend.dto.request.catalog.product.ProductUpdateRequest;
import com.example.backend.dto.response.catalog.ColorDto;
import com.example.backend.dto.response.catalog.SizeDto;
import com.example.backend.dto.response.catalog.product.ProductDetailResponse;
import com.example.backend.dto.response.catalog.product.ProductSummaryResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.excepton.BadRequestException;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.CategoryMapper;
import com.example.backend.mapper.ImageMapper;
import com.example.backend.mapper.ProductMapper;
import com.example.backend.mapper.ProductVariantMapper;
import com.example.backend.model.product.Category;
import com.example.backend.model.product.Product;
import com.example.backend.model.product.ProductImage;
import com.example.backend.model.product.ProductStatus;
import com.example.backend.repository.catalog.categoty.CategoryRepository;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.repository.catalog.product.ProductSpecification;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import com.example.backend.util.SlugUtil;
import lombok.RequiredArgsConstructor;
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
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;

    private final ProductMapper productMapper;
    private final ImageMapper imageMapper;
    private final ProductVariantMapper variantsMapper;
    private final CloudImageService imageService;
    private final CategoryMapper categoryMapper;

    private static final Set<String> ALLOWED_INCLUDES = Set.of("variants", "images", "categories", "options");

    private boolean slugExist(String slug, UUID excludedId) {
        return productRepository.existsBySlugIgnoreCaseAndIdNot(slug, excludedId);
    }

    public PageResponse<ProductSummaryResponse> list(ProductFilter filter, Pageable pageable) {
        Pageable effectivePageable = buildPageable(filter, pageable);
        Page<Product> page = productRepository.findAll(getFilterSpecification(filter), effectivePageable);
        if (page.isEmpty()) {
            return null;
        }
        List<UUID> productIds = page.getContent().stream().map(Product::getId).toList();
        Map<UUID, List<String>> colorsMap = new HashMap<>();
        productIds.forEach(id -> {
            List<ColorDto> colors = variantRepository.getColorsByProductId(id);
            colorsMap.put(id, colors.stream().map(ColorDto::getHexCode).toList());
        });

        return new PageResponse<>(page.map(p -> toSummaryResponse(p, colorsMap.getOrDefault(p.getId(), List.of()))));
    }

    public ProductDetailResponse create(ProductCreateRequest productCreateRequest) {
        Product product = productMapper.toEntity(productCreateRequest);
        String slug = SlugUtil.uniqueSlug(productCreateRequest.getSlug(), (v) -> this.slugExist(v, null));
        product.setSlug(slug);
        if (product.getSeoTitle().isBlank())
            product.setSeoTitle(product.getName());

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
    public ProductDetailResponse update(ProductUpdateRequest request, UUID id) {
        Product exist = productRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Product not found!"));
        if (StringUtils.hasText(request.getSlug()))
            request.setSlug(SlugUtil.uniqueSlug(request.getSlug(), (v) -> this.slugExist(v, id)));
        productMapper.updateFromDto(request, exist);

        // Handle categories
        if (request.getCategoryId() != null) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryId());
            exist.setCategories(categories);
        }

        if (request.getImages() != null) {
            List<ProductImage> images = request.getImages().stream().map(imageMapper::toEntity).toList();
            exist.syncImage(images);
        }
        exist = productRepository.save(exist);
        return productMapper.toDto(exist);
    }

    @Transactional
    public void delete(UUID productId) {
        Product exist = productRepository.findById(productId).orElseThrow(
                () -> new NotFoundException("Product not found!"));
        productRepository.delete(exist);
    }

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
                    () -> new NotFoundException("Product not found!"));
        } catch (Exception e) {
            product = productRepository.findBySlug(slugOrId).orElseThrow(
                    () -> new NotFoundException("Product not found!"));
        }
        Set<String> includeSet = normalizeIncludes(includes);
        ProductDetailResponse response = productMapper.toDto(product);

        if (includeSet.contains("variants")) {
            response.setVariants(variantsMapper.toResponse(product.getVariants()));
        }

        if (includeSet.contains("categories"))
            response.setCategories(categoryMapper.toDto(product.getCategories()));
        if (includeSet.contains("images"))
            response.setImages(imageMapper.toDto(product.getImages()));
        if (includeSet.contains("options")) {
            List<ColorDto> colorDto = variantRepository.getColorsByProductId(product.getId());
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
    public void changeStatus(UUID id, ProductStatus status) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Product not found!"));
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
        // Lọc trực tiếp trên Product (Category, Brand, Price)

        return Specification.allOf(
                ProductSpecification.hasStatus(filter.getStatus()),
                ProductSpecification.hasGender(filter.getGender()),
                ProductSpecification.hasCategory(filter.getCategoryId()),
                ProductSpecification.hasBrand(filter.getBrandId()),
                ProductSpecification.hasMinPrice(filter.getMinPriceAmount()),
                ProductSpecification.hasMaxPrice(filter.getMaxPriceAmount()),
                ProductSpecification.hasVariantSizes(filter.getSizeIds()),
                ProductSpecification.hasVariantColors(filter.getColorIds()));
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
        ;

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

    private ProductSummaryResponse toSummaryResponse(Product p, List<String> colors) {
        ProductSummaryResponse r = new ProductSummaryResponse();
        r.setId(p.getId());
        r.setSlug(p.getSlug());
        r.setColors(colors);
        r.setRatingAvg(p.getRatingAvg());
        r.setImageUrl(p.getPrimaryImageUrl());
        r.setName(p.getName());
        r.setStatus(p.getStatus());
        r.setGender(String.valueOf(p.getGender()));
        r.setPriceAmount(p.getPriceAmount());
        return r;
    }

}
