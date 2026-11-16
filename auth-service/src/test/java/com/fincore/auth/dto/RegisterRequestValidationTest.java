package com.fincore.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegisterRequest - Validações")
class RegisterRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest();
        request.setFullName("Gustavo Silva");
        request.setEmail("gustavo@fincore.com");
        request.setPassword("Senha@123");
        request.setCpf("52998224725");
        request.setTransactionPin("1234");
    }

    @Test
    @DisplayName("Deve aceitar cadastro válido")
    void validRequest() {
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar CPF inválido")
    void invalidCpf() {
        request.setCpf("11111111111");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cpf"));
    }

    @Test
    @DisplayName("Deve rejeitar senha fraca")
    void weakPassword() {
        request.setPassword("senha123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Deve exigir PIN de transação com exatamente 4 dígitos")
    void invalidTransactionPin() {
        request.setTransactionPin("12a4");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("transactionPin"));
    }

    @Test
    @DisplayName("Deve rejeitar telefone com quantidade inválida de dígitos")
    void invalidPhone() {
        request.setPhone("123");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }
}
