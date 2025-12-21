package com.example.bankcards.dto;

import com.example.bankcards.entity.Card;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private Long id;
    private String maskedCardNumber; // Маскированный номер: **** **** **** 1234
    private String cardholderName;
    private LocalDate expiryDate;
    private Card.CardStatus status;
    private BigDecimal balance;
    private Long ownerId;
    private String ownerUsername;

    public static CardResponse fromCard(Card card, String maskedNumber) {
        CardResponse response = new CardResponse();
        response.setId(card.getId());
        response.setMaskedCardNumber(maskedNumber);
        response.setCardholderName(card.getCardholderName());
        response.setExpiryDate(card.getExpiryDate());
        response.setStatus(card.getStatus());
        response.setBalance(card.getBalance());
        response.setOwnerId(card.getOwner().getId());
        response.setOwnerUsername(card.getOwner().getUsername());
        return response;
    }
}

