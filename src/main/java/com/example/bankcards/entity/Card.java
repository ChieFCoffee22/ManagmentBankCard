package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String encryptedCardNumber; // Зашифрованный номер карты

    @Column(nullable = false)
    private String cardholderName; // Владелец карты

    @Column(nullable = false)
    private LocalDate expiryDate; // Срок действия

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CardStatus {
        ACTIVE,      // Активна
        BLOCKED,     // Заблокирована
        EXPIRED      // Истек срок
    }

    // Метод для проверки срока действия
    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    // Автоматическое обновление статуса при изменении даты
    @PostLoad
    public void updateStatusIfExpired() {
        if (status != CardStatus.BLOCKED && isExpired()) {
            this.status = CardStatus.EXPIRED;
        }
    }
}

