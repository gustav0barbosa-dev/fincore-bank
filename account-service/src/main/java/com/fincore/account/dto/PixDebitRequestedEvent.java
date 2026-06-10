package com.fincore.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PixDebitRequestedEvent {
    private String transactionId;
    private Long senderAccountId;
    private String senderEmail;
    private String receiverKey;
    private Long receiverAccountId;
    private BigDecimal amount;
    private String description;
    private String failureReason;
}
