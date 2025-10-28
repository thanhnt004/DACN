package com.example.backend.service.discount;

import com.example.backend.dto.request.discount.*;
import com.example.backend.dto.response.discount.DiscountRedemptionResponse;
import com.example.backend.dto.response.discount.DiscountResponse;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.excepton.ConflictException;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.discount.DiscountMapper;
import com.example.backend.mapper.discount.DiscountRedemptionMapper;
import com.example.backend.model.DiscountRedemption;
import com.example.backend.model.discount.Discount;
import com.example.backend.model.product.Category;
import com.example.backend.model.product.Product;
import com.example.backend.repository.catalog.categoty.CategoryRepository;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.repository.discount.DiscountRedemptionRepository;
import com.example.backend.repository.discount.DiscountRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DiscountService {
    private final DiscountRepository discountRepository;
    private final DiscountRedemptionRepository discountRedemptionRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private final DiscountMapper discountMapper;
    private final DiscountRedemptionMapper redemptionMapper;
    public DiscountResponse create(DiscountCreateRequest request) {
        if (discountRepository.existsByCode(request.getCode()))
            throw new ConflictException("Discount code is existed");
        Discount newDiscount = discountMapper.toEntity(request);
        newDiscount = discountRepository.save(newDiscount);
        return discountMapper.toDto(newDiscount);
    }

    public void delete(UUID id) {
        Discount discount = findDiscountById(id);
        discountRepository.delete(discount);
    }
    public DiscountResponse update(UUID id, DiscountUpdateRequest request) {
        Discount discount = findDiscountById(id);
        discountMapper.updateEntityFromDto(request, discount);
        Discount updatedDiscount = discountRepository.save(discount);
        return discountMapper.toDto(updatedDiscount);
    }
    @Transactional(readOnly = true)
    public PageResponse<DiscountResponse> list(String code, Boolean active, Pageable pageable) {
        // Sử dụng Specification để tạo query động
        Specification<Discount> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (code != null && !code.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%"));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DiscountResponse> discountPage = discountRepository.findAll(spec, pageable).map(discountMapper::toDto);
        return new PageResponse<>(discountPage);
    }
    public void addProducts(UUID id, ProductAssignmentRequest request) {
        Discount discount = findDiscountById(id);
        List<Product> productsToAdd = productRepository.findAllById(request.getProductIds());
        discount.getProducts().addAll(productsToAdd);
        discountRepository.save(discount);
    }

    public void removeProducts(UUID id, ProductAssignmentRequest request) {
        Discount discount = findDiscountById(id);
        discount.getProducts().removeIf(product -> request.getProductIds().contains(product.getId()));
        discountRepository.save(discount);
    }

    public void addCategories(UUID id, CategoryAssignmentRequest request) {
        Discount discount = findDiscountById(id);
        List<Category> categoriesToAdd = categoryRepository.findAllById(request.getCategoryIds());
        discount.getCategories().addAll(categoriesToAdd);
        discountRepository.save(discount);
    }

    public void removeCategories(UUID id, CategoryAssignmentRequest request) {
        Discount discount = findDiscountById(id);
        discount.getCategories().removeIf(category -> request.getCategoryIds().contains(category.getId()));
        discountRepository.save(discount);
    }
    private Discount findDiscountById(UUID id) {
        return discountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Discount: " +id));
    }

    public DiscountResponse getById(UUID id) {
        return discountMapper.toDto(findDiscountById(id));
    }

    public PageResponse<DiscountRedemptionResponse> getRedemptions(UUID id, Pageable pageable) {
        if (!discountRepository.existsById(id)) {
            throw new NotFoundException("Discount: "+id);
        }
        Page<DiscountRedemptionResponse> page = discountRedemptionRepository.findByDiscountId(id,pageable).map(redemptionMapper::toDto);
        return new PageResponse<>(page);
    }
}
