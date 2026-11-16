package com.fincore.account.dto;

import com.fincore.account.model.PixKey;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PixKeyResponse {

    private Long id;
    private String keyType;
    private String keyValue;
    private Long accountId;
    private String accountNumber;
    private String ownerEmail;
    private LocalDateTime createdAt;

    public static PixKeyResponse from(PixKey key) {
        return PixKeyResponse.builder()
                .id(key.getId())
                .keyType(key.getKeyType().name())
                .keyValue(key.getKeyValue())
                .accountId(key.getAccount().getId())
                .accountNumber(key.getAccount().getAccountNumber())
                .ownerEmail(key.getOwnerEmail())
                .createdAt(key.getCreatedAt())
                .build();
    }
}
