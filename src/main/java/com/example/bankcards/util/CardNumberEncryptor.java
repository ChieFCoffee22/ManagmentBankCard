package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Утилита для шифрования и расшифровки номеров банковских карт.
 * Использует AES шифрование для защиты чувствительных данных.
 * 
 * @author system
 */
@Component
public class CardNumberEncryptor {
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    
    private final SecretKey secretKey;

    /**
     * Конструктор с инициализацией ключа шифрования.
     * Использует JWT secret для создания ключа (первые 32 байта).
     *
     * @param secret секретный ключ из конфигурации
     */
    public CardNumberEncryptor(@Value("${jwt.secret:mySecretKeyForJWTTokenGeneration12345678901234567890}") String secret) {
        // Используем JWT secret для создания ключа шифрования (первые 32 байта)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Если ключ короче, дополняем его
            byte[] extendedKey = new byte[32];
            System.arraycopy(keyBytes, 0, extendedKey, 0, Math.min(keyBytes.length, 32));
            this.secretKey = new SecretKeySpec(extendedKey, KEY_ALGORITHM);
        } else {
            this.secretKey = new SecretKeySpec(keyBytes, 0, 32, KEY_ALGORITHM);
        }
    }

    /**
     * Шифрует номер карты с использованием AES.
     *
     * @param cardNumber номер карты в открытом виде
     * @return зашифрованный номер карты (Base64)
     * @throws RuntimeException при ошибке шифрования
     */
    public String encrypt(String cardNumber) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting card number", e);
        }
    }

    /**
     * Расшифровывает номер карты.
     *
     * @param encryptedCardNumber зашифрованный номер карты (Base64)
     * @return номер карты в открытом виде
     * @throws RuntimeException при ошибке расшифровки
     */
    public String decrypt(String encryptedCardNumber) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedCardNumber));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting card number", e);
        }
    }
}

