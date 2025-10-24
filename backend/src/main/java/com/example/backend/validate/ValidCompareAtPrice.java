package com.example.backend.validate;

import com.example.backend.dto.request.catalog.product.VariantCreateRequest;
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
    class CompareAtPriceValidator implements ConstraintValidator<ValidCompareAtPrice, VariantCreateRequest> {

        @Override
        public boolean isValid(VariantCreateRequest value, ConstraintValidatorContext context) {
            if (value == null) return true;

            Long price = value.getPriceAmount();
            Long compareAt = value.getCompareAtAmount();

            if (compareAt == null) return true;

            return compareAt >= price;
        }
    }
}