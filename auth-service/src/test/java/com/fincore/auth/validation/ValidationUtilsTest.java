package com.fincore.auth.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationUtils - Testes Unitários")
class ValidationUtilsTest {

    @Test
    @DisplayName("Deve aceitar CPF válido, com ou sem máscara")
    void isValidCpf_valid() {
        assertThat(ValidationUtils.isValidCpf("52998224725")).isTrue();
        assertThat(ValidationUtils.isValidCpf("529.982.247-25")).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar CPF inválido ou com dígitos repetidos")
    void isValidCpf_invalid() {
        assertThat(ValidationUtils.isValidCpf(null)).isFalse();
        assertThat(ValidationUtils.isValidCpf("123")).isFalse();
        assertThat(ValidationUtils.isValidCpf("11111111111")).isFalse();
        assertThat(ValidationUtils.isValidCpf("52998224724")).isFalse();
    }

    @Test
    @DisplayName("Deve exigir senha forte")
    void isStrongPassword() {
        assertThat(ValidationUtils.isStrongPassword("Senha@123")).isTrue();
        assertThat(ValidationUtils.isStrongPassword("senha@123")).isFalse();
        assertThat(ValidationUtils.isStrongPassword("SENHA@123")).isFalse();
        assertThat(ValidationUtils.isStrongPassword("Senha1234")).isFalse();
        assertThat(ValidationUtils.isStrongPassword("Senha@")).isFalse();
        assertThat(ValidationUtils.isStrongPassword(null)).isFalse();
    }

    @Test
    @DisplayName("Deve normalizar e validar telefone")
    void phone() {
        assertThat(ValidationUtils.normalizePhone("(11) 99999-9999")).isEqualTo("11999999999");
        assertThat(ValidationUtils.isValidPhone("(11) 99999-9999")).isTrue();
        assertThat(ValidationUtils.isValidPhone("1199999999")).isTrue();
        assertThat(ValidationUtils.isValidPhone("123")).isFalse();
    }

    @Test
    @DisplayName("Deve validar e-mail")
    void email() {
        assertThat(ValidationUtils.isValidEmail("gustavo@fincore.com")).isTrue();
        assertThat(ValidationUtils.isValidEmail("invalido")).isFalse();
        assertThat(ValidationUtils.isValidEmail(null)).isFalse();
    }
}
