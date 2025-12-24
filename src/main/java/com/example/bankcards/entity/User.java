package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Сущность пользователя системы.
 * Хранит информацию о пользователе, его роли и карты.
 * 
 * @author system
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@ToString(exclude = {"password", "cards"})
@EqualsAndHashCode(exclude = {"roles", "cards"})
@NoArgsConstructor
@AllArgsConstructor
public class User {
    /**
     * Уникальный идентификатор пользователя.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальное имя пользователя для входа в систему.
     */
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * Хешированный пароль пользователя (BCrypt).
     */
    @Column(nullable = false)
    private String password;

    /**
     * Email адрес пользователя.
     */
    @Column(nullable = false)
    private String email;

    /**
     * Полное имя пользователя.
     */
    @Column(nullable = false)
    private String fullName;

    /**
     * Роли пользователя (связь Many-to-Many с Role).
     * Загружается сразу (EAGER) для Spring Security.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Карты пользователя (связь One-to-Many с Card).
     * При удалении пользователя удаляются все его карты.
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Card> cards = new HashSet<>();
}

