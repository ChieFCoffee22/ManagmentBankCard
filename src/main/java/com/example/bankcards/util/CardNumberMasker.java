package com.example.bankcards.util;

import org.springframework.stereotype.Component;

/**
 * Утилита для маскирования номеров банковских карт.
 * Скрывает большую часть номера, оставляя видимыми только последние 4 цифры.
 * 
 * @author system
 */
@Component
public class CardNumberMasker {
    
    /**
     * Маскирует номер карты, показывая только последние 4 цифры.
     * Формат результата: **** **** **** 1234
     *
     * @param cardNumber номер карты для маскирования
     * @return замаскированный номер карты
     */
    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        
        // Убираем все пробелы и нецифровые символы
        String digitsOnly = cardNumber.replaceAll("[^0-9]", "");
        
        if (digitsOnly.length() < 4) {
            return cardNumber;
        }
        
        // Берем последние 4 цифры
        String lastFour = digitsOnly.substring(digitsOnly.length() - 4);
        
        // Форматируем в формат **** **** **** 1234
        // Для стандартных карт (16 цифр) показываем 4 группы
        int groups = digitsOnly.length() == 16 ? 4 : (digitsOnly.length() / 4);
        StringBuilder masked = new StringBuilder();
        
        for (int i = 0; i < groups - 1; i++) {
            masked.append("****");
            if (i < groups - 2) {
                masked.append(" ");
            }
        }
        masked.append(" ").append(lastFour);
        
        return masked.toString();
    }
}

