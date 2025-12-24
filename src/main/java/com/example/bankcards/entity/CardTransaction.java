package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность транзакции между картами.
 * Хранит информацию о переводе средств с одной карты на другую.
 * 
 * @author system
 */
@Entity
@Table(name = "card_transactions")
@Getter
@Setter
@ToString(exclude = {"fromCard", "toCard"})
@EqualsAndHashCode(exclude = {"fromCard", "toCard", "transactionDate"})
@NoArgsConstructor
@AllArgsConstructor
public class CardTransaction {
    /**
     * Уникальный идентификатор транзакции.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Карта-источник перевода (связь Many-to-One с Card).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_card_id", nullable = false)
    private Card fromCard;

    /**
     * Карта-получатель перевода (связь Many-to-One с Card).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_card_id", nullable = false)
    private Card toCard;

    /**
     * Сумма перевода.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Дата и время выполнения транзакции (устанавливается автоматически).
     */
    @Column(nullable = false)
    private LocalDateTime transactionDate;

    /**
     * Устанавливает дату транзакции перед сохранением.
     */
    @PrePersist
    protected void onCreate() {
        transactionDate = LocalDateTime.now();
    }
}

