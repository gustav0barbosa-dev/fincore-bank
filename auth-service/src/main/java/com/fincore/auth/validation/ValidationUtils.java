package com.fincore.auth.validation;

import java.util.regex.Pattern;

/**
 * Utilitário central de validações de dados cadastrais.
 * Utilizado tanto pelas anotações customizadas quanto pela camada de serviço.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10,11}$");
    private static final Pattern UPPER_CASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_CASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[^A-Za-z0-9]");

    private ValidationUtils() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static String normalizeCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    public static boolean isValidCpf(String cpf) {
        String normalized = normalizeCpf(cpf);
        if (normalized == null || normalized.length() != 11) {
            return false;
        }
        // CPFs com todos os dígitos iguais (00000000000, 11111111111, ...) são inválidos
        if (normalized.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = calculateCpfDigit(normalized, 9);
        int secondDigit = calculateCpfDigit(normalized, 10);

        return normalized.charAt(9) - '0' == firstDigit
                && normalized.charAt(10) - '0' == secondDigit;
    }

    private static int calculateCpfDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (cpf.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return UPPER_CASE.matcher(password).find()
                && LOWER_CASE.matcher(password).find()
                && DIGIT.matcher(password).find()
                && SPECIAL_CHAR.matcher(password).find();
    }

    public static String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("\\D", "");
    }

    public static boolean isValidPhone(String phone) {
        String normalized = normalizePhone(phone);
        return normalized != null && PHONE_PATTERN.matcher(normalized).matches();
    }
}
