package com.example.bankcards.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardNumberMaskerTest {

    private CardNumberMasker cardNumberMasker;

    @BeforeEach
    void setUp() {
        cardNumberMasker = new CardNumberMasker();
    }

    @Test
    void testMaskCardNumber_Standard16Digits() {
        String cardNumber = "1234567890123456";
        String masked = cardNumberMasker.maskCardNumber(cardNumber);
        
        assertEquals("**** **** **** 3456", masked);
    }

    @Test
    void testMaskCardNumber_WithSpaces() {
        String cardNumber = "1234 5678 9012 3456";
        String masked = cardNumberMasker.maskCardNumber(cardNumber);
        
        assertEquals("**** **** **** 3456", masked);
    }

    @Test
    void testMaskCardNumber_ShortNumber() {
        String cardNumber = "123";
        String masked = cardNumberMasker.maskCardNumber(cardNumber);
        
        assertEquals(cardNumber, masked);
    }

    @Test
    void testMaskCardNumber_Null() {
        String masked = cardNumberMasker.maskCardNumber(null);
        
        assertNull(masked);
    }

    @Test
    void testMaskCardNumber_NonStandardLength() {
        String cardNumber = "12345678901234";
        String masked = cardNumberMasker.maskCardNumber(cardNumber);
        
        assertTrue(masked.contains("3456") || masked.endsWith("3456"));
    }
}

