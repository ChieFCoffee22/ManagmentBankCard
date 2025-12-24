package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Сущность роли пользователя.
 * Определяет права доступа пользователя в системе.
 * 
 * @author system
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    /**
     * Уникальный идентификатор роли.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название роли (ROLE_ADMIN или ROLE_USER).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleName name;

    /**
     * Доступные роли в системе.
     */
    public enum RoleName {
        /** Роль администратора с полным доступом. */
        ROLE_ADMIN,
        /** Роль обычного пользователя с ограниченным доступом. */
        ROLE_USER
    }
}

