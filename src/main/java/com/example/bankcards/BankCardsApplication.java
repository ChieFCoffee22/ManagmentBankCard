package com.example.bankcards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс приложения Bank Cards Management System.
 * REST API для управления банковскими картами с JWT аутентификацией.
 * 
 * <p>Приложение предоставляет следующие возможности:
 * <ul>
 *   <li>Аутентификация пользователей через JWT токены</li>
 *   <li>Управление банковскими картами (CRUD операции)</li>
 *   <li>Переводы между картами одного пользователя</li>
 *   <li>Ролевая модель доступа (ADMIN, USER)</li>
 *   <li>Шифрование номеров карт в базе данных</li>
 * </ul>
 * 
 * @author system
 * @version 1.0.0
 */
@SpringBootApplication
public class BankCardsApplication {

    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(BankCardsApplication.class, args);
    }
}

