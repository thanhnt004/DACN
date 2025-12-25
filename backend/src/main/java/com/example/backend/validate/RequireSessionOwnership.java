package com.example.backend.validate;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSessionOwnership {
    String message() default "Session ownership validation failed";
}
