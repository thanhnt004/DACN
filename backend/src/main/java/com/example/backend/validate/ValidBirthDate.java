package com.example.backend.validate;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.Period;

@Target({ElementType.FIELD,ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidBirthDate.BirthDateValidator.class)
public @interface ValidBirthDate {
    String message() default "Invalid birthdate!";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    int minAge() default 16;
    int maxAge() default 120;

    class BirthDateValidator implements ConstraintValidator<ValidBirthDate, LocalDate>
    {
        private int minAge;
        private int maxAge;
        @Override
        public void initialize(ValidBirthDate constraintAnnotation) {
            this.minAge = constraintAnnotation.minAge();
            this.maxAge = constraintAnnotation.maxAge();
            ConstraintValidator.super.initialize(constraintAnnotation);
        }

        @Override
        public boolean isValid(LocalDate birthDate, ConstraintValidatorContext constraintValidatorContext) {
            if (birthDate == null) {
                return false;
            }

            LocalDate now = LocalDate.now();

            // Kiểm tra ngày sinh không được trong tương lai
            if (birthDate.isAfter(now)) {
                return false;
            }

            // Tính tuổi
            int age = Period.between(birthDate, now).getYears();

            // Kiểm tra tuổi tối thiểu và tối đa
            return age >= minAge && age <= maxAge;
        }
    }
}
