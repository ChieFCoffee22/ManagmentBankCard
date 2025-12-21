package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private Long id;
    private Long fromCardId;
    private String fromCardMaskedNumber;
    private Long toCardId;
    private String toCardMaskedNumber;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String message;
}

