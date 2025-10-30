package com.example.backend.validate;

import com.example.backend.dto.request.catalog.product.VariantCreateRequest;
import com.example.backend.dto.request.catalog.product.VariantUpdateRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidCompareAtPrice.CompareAtPriceValidator.class)
@Target({ ElementType.TYPE })   // áp dụng cho class
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCompareAtPrice {
    String message() default "compareAtAmount phải >= priceAmount";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    class CompareAtPriceValidator implements ConstraintValidator<ValidCompareAtPrice, Object> {
        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) return true;

            Long price = null;
            Long compareAt = null;

            if (value instanceof VariantCreateRequest) {
                VariantCreateRequest dto = (VariantCreateRequest) value;
                price = dto.getPriceAmount();
                compareAt = dto.getCompareAtAmount();
            } else if (value instanceof VariantUpdateRequest) {
                VariantUpdateRequest dto = (VariantUpdateRequest) value;
                price = dto.getPriceAmount();
                compareAt = dto.getCompareAtAmount();
            } else {
                // nếu dùng cho kiểu khác, coi như hợp lệ hoặc xử lý tuỳ ý
                return true;
            }

            if (compareAt == null || price == null) return true;
            return compareAt >= price;
        }
    }
}