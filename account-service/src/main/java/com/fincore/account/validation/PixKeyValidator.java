package com.fincore.account.validation;

import com.fincore.account.exception.BusinessException;
import com.fincore.account.model.PixKeyType;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Normalização e validação das chaves PIX suportadas pelo FinCore.
 */
public final class PixKeyValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10,11}$");
    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");

    private PixKeyValidator() {
    }

    public static String normalize(PixKeyType type, String value) {
        if (type == PixKeyType.RANDOM && (value == null || value.isBlank())) {
            return UUID.randomUUID().toString();
        }
        if (value == null) {
            throw new BusinessException("Valor da chave PIX é obrigatório");
        }

        return switch (type) {
            case EMAIL, RANDOM -> value.trim().toLowerCase();
            case CPF, PHONE -> value.replaceAll("\\D", "");
        };
    }

    public static void validate(PixKeyType type, String normalizedValue) {
        switch (type) {
            case CPF -> {
                if (!CPF_PATTERN.matcher(normalizedValue).matches() || !isValidCpf(normalizedValue)) {
                    throw new BusinessException("CPF inválido para chave PIX");
                }
            }
            case EMAIL -> {
                if (!EMAIL_PATTERN.matcher(normalizedValue).matches()) {
                    throw new BusinessException("E-mail inválido para chave PIX");
                }
            }
            case PHONE -> {
                if (!PHONE_PATTERN.matcher(normalizedValue).matches()) {
                    throw new BusinessException("Telefone inválido para chave PIX: informe 10 ou 11 dígitos");
                }
            }
            case RANDOM -> {
                try {
                    UUID.fromString(normalizedValue);
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("Chave aleatória inválida: informe um UUID");
                }
            }
        }
    }

    /**
     * Normaliza uma chave informada em uma transferência, sem conhecer previamente o tipo.
     */
    public static String normalizeForLookup(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new BusinessException("Chave PIX do destinatário é obrigatória");
        }
        String value = rawKey.trim();
        if (value.contains("@")) {
            return value.toLowerCase();
        }

        // CPF/telefone podem chegar formatados (529.982.247-25, (11) 99999-9999)
        String digits = value.replaceAll("\\D", "");
        if ((digits.length() == 10 || digits.length() == 11)
                && value.matches("[\\d.\\-()\\s]+")) {
            return digits;
        }

        return value.toLowerCase();
    }

    public static boolean isValidCpf(String cpf) {
        if (cpf == null || cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        int firstDigit = calculateCpfDigit(cpf, 9);
        int secondDigit = calculateCpfDigit(cpf, 10);
        return cpf.charAt(9) - '0' == firstDigit && cpf.charAt(10) - '0' == secondDigit;
    }

    private static int calculateCpfDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (cpf.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
