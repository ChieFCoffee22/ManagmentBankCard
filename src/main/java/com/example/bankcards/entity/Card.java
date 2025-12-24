package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Сущность банковской карты.
 * Хранит информацию о карте, включая зашифрованный номер, баланс и статус.
 * 
 * @author system
 */
@Entity
@Table(name = "cards")
@Getter
@Setter
@ToString(exclude = {"encryptedCardNumber", "owner"})
@EqualsAndHashCode(exclude = {"owner", "createdAt", "updatedAt"})
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    /**
     * Уникальный идентификатор карты.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Зашифрованный номер карты (AES шифрование).
     * Никогда не хранится в открытом виде.
     */
    @Column(nullable = false, unique = true, length = 500)
    private String encryptedCardNumber;

    /**
     * Имя держателя карты.
     */
    @Column(nullable = false)
    private String cardholderName;

    /**
     * Срок действия карты.
     */
    @Column(nullable = false)
    private LocalDate expiryDate;

    /**
     * Текущий статус карты (ACTIVE, BLOCKED, EXPIRED).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    /**
     * Текущий баланс карты.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Владелец карты (связь Many-to-One с User).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Дата и время создания карты (устанавливается автоматически).
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления карты (обновляется автоматически).
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Устанавливает дату создания и обновления перед сохранением новой карты.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Обновляет дату изменения перед обновлением карты.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Статусы карты.
     */
    public enum CardStatus {
        /** Карта активна и может использоваться. */
        ACTIVE,
        /** Карта заблокирована пользователем или администратором. */
        BLOCKED,
        /** Срок действия карты истек. */
        EXPIRED
    }

    /**
     * Проверяет, истек ли срок действия карты.
     *
     * @return true, если срок действия истек, иначе false
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    /**
     * Автоматически обновляет статус карты на EXPIRED при загрузке из БД,
     * если срок действия истек и карта не заблокирована.
     */
    @PostLoad
    public void updateStatusIfExpired() {
        if (status != CardStatus.BLOCKED && isExpired()) {
            this.status = CardStatus.EXPIRED;
        }
    }
}

