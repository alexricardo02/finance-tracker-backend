package com.example.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SupportedCurrencyValidator implements ConstraintValidator<SupportedCurrency, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || com.example.models.SupportedCurrency.isSupported(value);
    }
}
