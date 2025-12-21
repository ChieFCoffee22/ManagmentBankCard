package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardTransactionRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumberEncryptor;
import com.example.bankcards.util.CardNumberMasker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardTransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardNumberEncryptor cardNumberEncryptor;

    @Mock
    private CardNumberMasker cardNumberMasker;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransferService transferService;

    private User user;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setOwner(user);
        fromCard.setStatus(Card.CardStatus.ACTIVE);
        fromCard.setBalance(new BigDecimal("1000.00"));
        fromCard.setExpiryDate(LocalDate.now().plusYears(2));

        toCard = new Card();
        toCard.setId(2L);
        toCard.setOwner(user);
        toCard.setStatus(Card.CardStatus.ACTIVE);
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setExpiryDate(LocalDate.now().plusYears(2));

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    void testTransferBetweenOwnCards_Success() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("200.00"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardNumberEncryptor.decrypt(anyString())).thenReturn("1234567890123456");
        when(cardNumberMasker.maskCardNumber(anyString())).thenReturn("**** **** **** 3456");

        // Act
        var result = transferService.transferBetweenOwnCards(request);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.getAmount());
        assertEquals(1L, result.getFromCardId());
        assertEquals(2L, result.getToCardId());
        assertEquals(new BigDecimal("800.00"), fromCard.getBalance());
        assertEquals(new BigDecimal("700.00"), toCard.getBalance());
        verify(cardRepository, times(2)).save(any(Card.class));
        verify(transactionRepository, times(1)).save(any());
    }

    @Test
    void testTransferBetweenOwnCards_InsufficientFunds() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("2000.00"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> transferService.transferBetweenOwnCards(request));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void testTransferBetweenOwnCards_SameCard() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(1L);
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> transferService.transferBetweenOwnCards(request));
    }

    @Test
    void testTransferBetweenOwnCards_CardNotFound() {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromCardId(999L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> transferService.transferBetweenOwnCards(request));
    }

    @Test
    void testTransferBetweenOwnCards_CardNotActive() {
        // Arrange
        fromCard.setStatus(Card.CardStatus.BLOCKED);
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> transferService.transferBetweenOwnCards(request));
    }
}

