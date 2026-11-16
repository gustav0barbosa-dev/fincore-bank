package com.fincore.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ValidateTransactionPinRequest {

    @NotBlank(message = "PIN de transação é obrigatório")
    @Pattern(regexp = "^\\d{4}$", message = "PIN de transação deve conter exatamente 4 dígitos")
    private String pin;
}
