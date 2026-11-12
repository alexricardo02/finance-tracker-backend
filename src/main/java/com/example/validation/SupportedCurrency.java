package com.example.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The value must be the exact code of a {@link com.example.models.SupportedCurrency}.
 * Null is valid; combine with {@code @NotNull} or {@code @NotBlank} when the field is required.
 */
@Documented
@Constraint(validatedBy = SupportedCurrencyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedCurrency {

    String message() default "Unsupported currency";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
