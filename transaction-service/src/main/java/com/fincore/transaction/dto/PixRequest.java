package com.fincore.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PixRequest {
    @NotNull
    private Long senderAccountId;

    @NotBlank(message = "Chave PIX do destinatário é obrigatória")
    private String receiverKey;

    @NotNull @DecimalMin("0.01")
    private BigDecimal amount;

    private String description;

    @NotBlank(message = "PIN de transação é obrigatório")
    @Pattern(regexp = "^\\d{4}$", message = "PIN de transação deve conter exatamente 4 dígitos")
    private String transactionPin;
}
