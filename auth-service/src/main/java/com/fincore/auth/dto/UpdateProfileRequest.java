package com.fincore.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 3, max = 255, message = "Nome completo deve ter entre 3 e 255 caracteres")
    private String fullName;

    @Pattern(regexp = "^$|^\\d{10,11}$", message = "Telefone deve conter 10 ou 11 dígitos")
    private String phone;
}
