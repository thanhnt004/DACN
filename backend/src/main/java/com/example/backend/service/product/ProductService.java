package com.example.backend.service.product;

import com.example.backend.dto.ProductImageDto;
import com.example.backend.dto.request.product.ProductCreateRequest;
import com.example.backend.dto.request.product.ProductUpdateRequest;
import com.example.backend.dto.request.product.ProductView;
import com.example.backend.dto.response.product.ProductResponse;
import com.example.backend.excepton.BadRequestException;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.CategoryMapper;
import com.example.backend.mapper.ImageMapper;
import com.example.backend.mapper.ProductMapper;
import com.example.backend.mapper.ProductVariantMapper;
import com.example.backend.model.product.Gender;
import com.example.backend.model.product.Product;
import com.example.backend.model.product.ProductImage;
import com.example.backend.model.product.ProductStatus;
import com.example.backend.repository.product.ProductRepository;
import com.example.backend.repository.product.ProductVariantRepository;
import com.example.backend.util.SlugUtil;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    private final ProductMapper productMapper;
    private final ImageMapper imageMapper;
    private final ProductVariantMapper variantsMapper;
    private final CloudImageService imageService;
    private final CategoryMapper categoryMapper;

    private static final Set<String> ALLOWED_INCLUDES = Set.of("variants", "images", "categories");

    private boolean ensureUniqueSlug(String slug, UUID excludedId)
    {
        return !productRepository.existsBySlugIgnoreCaseAndIdNot(slug, excludedId);
    }
    @Transactional
    public ProductResponse create(ProductCreateRequest productCreateRequest)
    {
        Product product = productMapper.toEntity(productCreateRequest);
        String slug = SlugUtil.uniqueSlug(productCreateRequest.getSlug(),(v)->this.ensureUniqueSlug(v,null));
        product.setSlug(slug);
        if (product.getSeoTitle().isBlank())
            product.setSeoTitle(product.getName());
        if (productCreateRequest.getImages()!=null)
        {
            for (ProductImageDto imageDto: productCreateRequest.getImages())
            {
                product.addImage(imageMapper.toEntity(imageDto));
            }
        }
        product = productRepository.save(product);
        return productMapper.toDto(product);
    }
    @Transactional
    public ProductResponse update(ProductUpdateRequest request)
    {
        Product exist = productRepository.findById(request.getId()).orElseThrow(
                ()->new NotFoundException("Product not found!")
        );
        if (StringUtils.hasText(request.getSlug()))
            request.setSlug(SlugUtil.uniqueSlug(request.getSlug(),(v)->this.ensureUniqueSlug(v,request.getId())));
        productMapper.updateFromDto(request,exist);
        if (request.getImages()!=null)
        {
            List<ProductImage> images = request.getImages().stream().map(imageMapper::toEntity).toList();
            exist.syncImage(images);
        }
        exist = productRepository.save(exist);
        return productMapper.toDto(exist);
    }
    @Transactional
    public void delete(UUID productId)
    {
        Product exist = productRepository.findById(productId).orElseThrow(
                ()->new NotFoundException("Product not found!")
        );
        productRepository.delete(exist);
    }
    //get detail
    @Transactional
    public ProductResponse findById(UUID productId, List<String> include)
    {
        Set<String> includes = normalizeIncludes(include);
        List<String> unknown = includes.stream()
                .filter(s -> !ALLOWED_INCLUDES.contains(s))
                .toList();
        if(!unknown.isEmpty())
            throw new BadRequestException("Invalid include value: "+ String.join(", ", unknown)
                    + ". Allow: " + String.join(", ", ALLOWED_INCLUDES));
        Product product = productRepository.findById(productId).orElseThrow(
                ()->new NotFoundException("Product not found!")
        );

        ProductResponse response = productMapper.toDto(product);
        if (includes.contains("variants"))
            response.setVariants(variantsMapper.toDto(product.getVariants()));
        if (includes.contains("categories"))
            response.setCategories(categoryMapper.toDto(product.getCategories()));
        if (includes.contains("images"))
            response.setImages(imageMapper.toDto(product.getImages()));
        return response;
    }
    @Transactional
    public List<ProductView> getProductViews() {
        List<ProductView> products = productRepository.findAllProductViews();
        List<UUID> ids = products.stream().map(ProductView::getId).toList();

        // fetch colors grouped by productId
        List<Object[]> rows = productRepository.findColorsByProductIds(ids);
        Map<UUID, List<String>> colorMap = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> (UUID) r[0],
                        Collectors.mapping(r -> (String) r[1], Collectors.toList())
                ));

        // assign color list
        products.forEach(p -> p.setColorHex(colorMap.getOrDefault(p.getId(), List.of())));
        return products;
    }
    @Transactional
    public Page<ProductView> search(String q,
                                    UUID brandId,
                                    UUID categoryId,
                                    String status,
                                    BigInteger minPrice,
                                    BigInteger maxPrice,
                                    Gender gender,
                                    int page,
                                    int size,
                                    List<Sort.Order> sortOrders)
    {
        Specification<Product> spec = withFilters(q,brandId, categoryId, status, minPrice, maxPrice, gender);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrders == null ? List.of() : sortOrders));
        List<UUID> ids = productRepository.findAll(spec, pageable).getContent().stream().map(Product::getId).toList();
        long total = productRepository.count(spec);

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        // 3) fetch aggregated DTO rows bằng native query
        List<Object[]> rows = productRepository.findProductViewsByIds(ids);

        // 4) map rows -> ProductView
        Map<UUID, ProductView> map = new HashMap<>();
        for (Object[] r : rows) {
            UUID id = (UUID) r[0];
            String imageUrl = (String) r[1];
            String name = (String) r[2];
            String colorsCsv = (String) r[3];
            String genderStr = r[4] == null ? null : r[4].toString();
            Object priceObj = r[5];

            List<String> colors = colorsCsv == null ? List.of() :
                    Arrays.stream(colorsCsv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

            Gender g = genderStr == null ? null : Gender.valueOf(genderStr);
            String price = priceObj == null ? null : priceObj.toString();

            map.put(id, new ProductView(id, imageUrl, name, colors, g, price));
        }

        // 5) preserve order according to ids
        List<ProductView> content = ids.stream().map(map::get).filter(Objects::nonNull).collect(Collectors.toList());

        return new PageImpl<>(content, pageable, total);
    }
    public void addImage(UUID productId, ProductImageDto imageDto)
    {
        Product product = productRepository.findById(productId).orElseThrow(
                ()->new NotFoundException("Product not found!")
        );
        product.addImage(imageMapper.toEntity(imageDto));
    }
    public void removeImage(UUID productId, UUID imageId)  {
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new NotFoundException("Product not found!")
        );
        List<ProductImage> productImages = product.getImages();
        ProductImage toRemove = productImages.stream().filter(img->img.getId().equals(imageId)).toList().getFirst();
        product.removeImage(toRemove);
    }
        private Set<String> normalizeIncludes(List<String> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptySet();
        return raw.stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    public  Specification<Product> withFilters(
            String q,
            UUID brandId,
            UUID categoryId,
            String status,
            BigInteger minPrice,
            BigInteger maxPrice,
            Gender gender) {
        return (root, query, cb) -> {
            // prevent duplicates when joining to collections
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("brand", JoinType.LEFT);
                root.fetch("category", JoinType.LEFT);
            }

            Predicate p = cb.conjunction();

            if (StringUtils.hasText(q))
                p = cb.and(p,cb.like(root.get("name"),q));
            if (brandId != null) {
                p = cb.and(p, cb.equal(root.get("brand").get("id"), brandId));
            }
            if (categoryId != null) {
                p = cb.and(p, cb.equal(root.get("category").get("id"), categoryId));
            }
            if (status != null && !status.isBlank()) {
                p = cb.and(p, cb.equal(root.get("status"), status));
            }
            if (gender != null) {
                p = cb.and(p, cb.equal(root.get("gender"), gender));
            }
            if (minPrice != null) {
                p = cb.and(p, cb.ge(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                p = cb.and(p, cb.le(root.get("price"), maxPrice));
            }
            return p;
        };
    }

    @Transactional
    public void changeStatus(UUID id, ProductStatus status) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Product not found!")
        );
        if (product.getStatus().equals(status))
            return;
        if (status.equals(ProductStatus.ACTIVE))
            validateProduct(product);
        else
            product.setStatus(status);
        productRepository.save(product);
    }

    private void validateProduct(Product product) {
        if (variantRepository.countByProductId(product.getId())<=0)
            throw new BadRequestException("Product must have at least one variant!");
    }

}
