package com.fincore.account.dto;

import com.fincore.account.model.PixKeyType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePixKeyRequest {

    @NotNull(message = "Conta é obrigatória")
    private Long accountId;

    @NotNull(message = "Tipo da chave PIX é obrigatório")
    private PixKeyType keyType;

    /** Obrigatório para CPF, e-mail e telefone. Para RANDOM, se vazio, o sistema gera um UUID. */
    private String keyValue;
}
