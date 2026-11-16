package com.fincore.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangeTransactionPinRequest {

    @NotBlank(message = "Senha de acesso é obrigatória")
    private String currentPassword;

    @NotBlank(message = "Novo PIN de transação é obrigatório")
    @Pattern(regexp = "^\\d{4}$", message = "PIN de transação deve conter exatamente 4 dígitos")
    private String newPin;
}
