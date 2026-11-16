package com.fincore.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CPFValidator implements ConstraintValidator<CPF, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // @NotBlank/@NotNull cuidam da obrigatoriedade; aqui validamos apenas o formato/verificadores
        if (value == null || value.isBlank()) {
            return true;
        }
        return ValidationUtils.isValidCpf(value);
    }
}
