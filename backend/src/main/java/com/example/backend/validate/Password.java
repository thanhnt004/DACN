package com.example.backend.validate;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Constraint(validatedBy = Password.StrongPasswordValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Password {
    String message() default "Password must contain at least one lowercase letter, one uppercase letter, and one digit";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class StrongPasswordValidator implements ConstraintValidator<Password, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value != null && value.matches("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!*()]).{8,}$");
        }
    }
}