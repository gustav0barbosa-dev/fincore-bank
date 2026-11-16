package com.fincore.auth.dto;

import com.fincore.auth.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private String cpf;
    private String phone;
    private String role;
    private boolean enabled;
    private boolean transactionPinConfigured;
    private LocalDateTime createdAt;

    public static ProfileResponse from(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .cpf(user.getCpf())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .transactionPinConfigured(user.getTransactionPin() != null && !user.getTransactionPin().isBlank())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
