package com.fincore.auth.dto;

import com.fincore.auth.validation.CPF;
import com.fincore.auth.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 3, max = 255, message = "Nome completo deve ter entre 3 e 255 caracteres")
    private String fullName;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @StrongPassword
    private String password;

    @NotBlank(message = "CPF é obrigatório")
    @CPF
    private String cpf;

    @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve conter 10 ou 11 dígitos")
    private String phone;

    @NotBlank(message = "Senha de transação é obrigatória")
    @Pattern(regexp = "^\\d{4}$", message = "Senha de transação deve conter exatamente 4 dígitos")
    private String transactionPin;
}
