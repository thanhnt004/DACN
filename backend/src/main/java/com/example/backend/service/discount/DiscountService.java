package com.example.backend.service.discount;

import com.example.backend.dto.request.discount.*;
import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.discount.DiscountRedemptionResponse;
import com.example.backend.dto.response.discount.DiscountResponse;
import com.example.backend.dto.response.discount.DiscountResult;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.excepton.ConflictException;
import com.example.backend.excepton.NotFoundException;
import com.example.backend.mapper.discount.DiscountMapper;
import com.example.backend.mapper.discount.DiscountRedemptionMapper;
import com.example.backend.model.discount.Discount;
import com.example.backend.model.product.Category;
import com.example.backend.model.product.Product;
import com.example.backend.repository.catalog.categoty.CategoryRepository;
import com.example.backend.repository.catalog.product.ProductRepository;
import com.example.backend.repository.discount.DiscountRedemptionRepository;
import com.example.backend.repository.discount.DiscountRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    //Validate va tinh toan giam gia
    public DiscountResult validateAndCalculate(@Pattern(
            regexp = "^[A-Z0-9]{4,20}$",
            message = "Mã giảm giá phải từ 4-20 ký tự chữ hoa và số"
    ) String discountCode, Long subtotalAmount, List<CheckoutItemDetail> items, UUID userId) {
        Discount discount = discountRepository.findByCode(discountCode)
                .orElseThrow(() -> new NotFoundException("Discount code not found: " + discountCode));
        //Kiem tra hop le
        if (!discount.isActive())
            return DiscountResult.invalid("Discount code is not active: " + discount.getCode());

        Optional<DiscountResult> inValidResult = validateDiscountCode(discount, subtotalAmount, items, userId);
        if (inValidResult.isPresent())
            return inValidResult.get();
        //Tinh toan giam gia
        Long discountAmount = switch (discount.getType()) {
            case PERCENTAGE -> discount.getValue()*subtotalAmount/100;
            case FIXED_AMOUNT -> discount.getValue()>subtotalAmount? subtotalAmount: discount.getValue();
        };
        return DiscountResult.builder()
                .discountId(discount.getId())
                .isValid(true)
                .discountAmount(discountAmount)
                .type(String.valueOf(discount.getType()))
                .code(discountCode)
                .value(discount.getValue())
                .build();
    }
    private Optional<DiscountResult> validateDiscountCode(@Pattern(
            regexp = "^[A-Z0-9]{4,20}$",
            message = "Mã giảm giá phải từ 4-20 ký tự chữ hoa và số"
    ) Discount discount, Long subtotalAmount, List<CheckoutItemDetail> items, UUID userId) {

        //Kiem tra hop le
        if (!discount.isActive())
            return Optional.of(DiscountResult.invalid("Discount code is not active: " + discount.getCode()));
        if (discount.getStartsAt() != null && discount.getStartsAt().isAfter(LocalDateTime.now()))
            return Optional.of(DiscountResult.invalid("Discount code is not started yet: "  + discount.getCode()));
        if (discount.getEndsAt() != null && discount.getEndsAt().isBefore(LocalDateTime.now()))
            return Optional.of(DiscountResult.invalid("Discount code is expired: "  + discount.getCode()));
        if (discount.getMaxRedemptions()!=null)
        {
            Long totalRedemptions = discountRedemptionRepository.countByDiscountId(discount.getId());
            if (totalRedemptions >= discount.getMaxRedemptions())
                return Optional.of(DiscountResult.invalid("Discount code has reached maximum redemptions: "  + discount.getCode()));
        }
        if (userId != null && discount.getPerUserLimit() != null) {
            long userUsed = discountRedemptionRepository.countByDiscountIdAndUserId(
                    discount.getId(), userId
            );
            if (userUsed >= discount.getPerUserLimit()) {
                return Optional.of(DiscountResult.invalid("You have used up this discount code: "  + discount.getCode()));
            }
        }
        if (discount.getMinOrderAmount() != null
                && subtotalAmount < discount.getMinOrderAmount()) {
            return Optional.of(DiscountResult.invalid(String.format(" Minimum order of %,d VND to use this code",discount.getMinOrderAmount())));
        }
        if (!validateScope(discount, items))
            return Optional.of(DiscountResult.invalid("Not applicable to selected products " ));
        return Optional.empty();
    }
    private boolean validateScope(Discount discount, List<CheckoutItemDetail> items) {
        boolean appliesToAllProducts = (discount.getProducts().isEmpty() && discount.getCategories().isEmpty());
        if (appliesToAllProducts) {
            return true;
        }
        boolean valid = false;
        for (CheckoutItemDetail item : items) {
            UUID variantProductId = item.getProductId();
            // Check if the product is directly associated with the discount
            if (discount.getProducts().stream().anyMatch(p -> p.getId().equals(variantProductId))) {
                valid = true;
                break;
            }
            // Check if the product's categories are associated with the discount
            Product product = productRepository.findById(variantProductId).orElse(null);
            if (product != null) {
                for (Category category : product.getCategories()) {
                    if (discount.getCategories().stream().anyMatch(c -> c.getId().equals(category.getId()))) {
                        valid = true;
                        break;
                    }
                }
            }
            if (valid) {
                break;
            }
        }
        return valid;
    }
}
