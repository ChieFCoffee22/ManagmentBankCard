package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CardStatusUpdateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumberEncryptor;
import com.example.bankcards.util.CardNumberMasker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для управления банковскими картами.
 * Реализует бизнес-логику работы с картами: создание, обновление, удаление, получение.
 * 
 * @author system
 */
@Service
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardNumberEncryptor cardNumberEncryptor;
    private final CardNumberMasker cardNumberMasker;

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param cardRepository репозиторий карт
     * @param userRepository репозиторий пользователей
     * @param cardNumberEncryptor утилита для шифрования номеров карт
     * @param cardNumberMasker утилита для маскирования номеров карт
     */
    public CardService(CardRepository cardRepository,
                      UserRepository userRepository,
                      CardNumberEncryptor cardNumberEncryptor,
                      CardNumberMasker cardNumberMasker) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.cardNumberEncryptor = cardNumberEncryptor;
        this.cardNumberMasker = cardNumberMasker;
    }

    /**
     * Получает список карт пользователя с фильтрацией и пагинацией.
     * Обычные пользователи могут видеть только свои карты, администраторы - любые.
     *
     * @param userId ID пользователя
     * @param cardholderName фильтр по имени держателя карты (опционально)
     * @param status фильтр по статусу карты (опционально)
     * @param pageable параметры пагинации
     * @return страница с картами пользователя
     * @throws ForbiddenException если обычный пользователь пытается посмотреть чужие карты
     */
    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCardsForUser(Long userId, String cardholderName, Card.CardStatus status, Pageable pageable) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Админ может видеть все карты, обычный пользователь только свои
        if (!currentUser.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"))) {
            if (!userId.equals(currentUser.getId())) {
                throw new ForbiddenException("Access denied: You can only view your own cards");
            }
        }

        Page<Card> cards;
        if (cardholderName != null && !cardholderName.isEmpty() || status != null) {
            cards = cardRepository.findByOwnerIdWithFilters(userId, cardholderName, status, pageable);
        } else {
            cards = cardRepository.findByOwnerId(userId, pageable);
        }

        return cards.map(card -> {
            String maskedNumber = cardNumberMasker.maskCardNumber(
                    cardNumberEncryptor.decrypt(card.getEncryptedCardNumber()));
            return CardResponse.fromCard(card, maskedNumber);
        });
    }

    /**
     * Получает карту по ID.
     * Обычные пользователи могут получить только свои карты, администраторы - любые.
     *
     * @param cardId идентификатор карты
     * @return информация о карте
     * @throws ResourceNotFoundException если карта не найдена
     * @throws ForbiddenException если обычный пользователь пытается получить чужую карту
     */
    @Transactional(readOnly = true)
    public CardResponse getCardById(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Проверка доступа
        if (!currentUser.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"))) {
            if (!card.getOwner().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("Access denied: You can only view your own cards");
            }
        }

        String maskedNumber = cardNumberMasker.maskCardNumber(
                cardNumberEncryptor.decrypt(card.getEncryptedCardNumber()));
        return CardResponse.fromCard(card, maskedNumber);
    }

    /**
     * Создает новую банковскую карту.
     * Обычные пользователи могут создавать карты только для себя, администраторы - для любого пользователя.
     *
     * @param request данные для создания карты
     * @return созданная карта
     * @throws BadRequestException если срок действия в прошлом или номер карты уже существует
     * @throws ForbiddenException если обычный пользователь пытается создать карту для другого пользователя
     */
    @Transactional
    public CardResponse createCard(CardCreateRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Админ может создавать карты для любого пользователя, обычный пользователь только для себя
        User owner;
        if (request.getOwnerId() != null) {
            if (!currentUser.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"))) {
                throw new ForbiddenException("Only admins can create cards for other users");
            }
            owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Owner not found with id: " + request.getOwnerId()));
        } else {
            owner = currentUser;
        }

        // Проверка срока действия
        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Expiry date cannot be in the past");
        }

        // Проверка уникальности номера карты
        String encryptedCardNumber = cardNumberEncryptor.encrypt(request.getCardNumber());
        if (cardRepository.findAll().stream()
                .anyMatch(c -> c.getEncryptedCardNumber().equals(encryptedCardNumber))) {
            throw new BadRequestException("Card with this number already exists");
        }

        Card card = new Card();
        card.setEncryptedCardNumber(encryptedCardNumber);
        card.setCardholderName(request.getCardholderName());
        card.setExpiryDate(request.getExpiryDate());
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setBalance(java.math.BigDecimal.ZERO);
        card.setOwner(owner);

        Card savedCard = cardRepository.save(card);

        String maskedNumber = cardNumberMasker.maskCardNumber(request.getCardNumber());
        return CardResponse.fromCard(savedCard, maskedNumber);
    }

    /**
     * Обновляет статус карты.
     * Обычные пользователи могут только заблокировать свои карты, администраторы - изменить любой статус.
     *
     * @param cardId идентификатор карты
     * @param request новый статус карты
     * @return обновленная карта
     * @throws ResourceNotFoundException если карта не найдена
     * @throws ForbiddenException если обычный пользователь пытается изменить статус чужой карты или установить статус, отличный от BLOCKED
     */
    @Transactional
    public CardResponse updateCardStatus(Long cardId, CardStatusUpdateRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Обычный пользователь может только запросить блокировку
        if (!currentUser.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"))) {
            if (!card.getOwner().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("Access denied: You can only manage your own cards");
            }
            if (request.getStatus() != Card.CardStatus.BLOCKED) {
                throw new ForbiddenException("You can only request to block your card");
            }
        }

        card.setStatus(request.getStatus());
        Card updatedCard = cardRepository.save(card);

        String maskedNumber = cardNumberMasker.maskCardNumber(
                cardNumberEncryptor.decrypt(updatedCard.getEncryptedCardNumber()));
        return CardResponse.fromCard(updatedCard, maskedNumber);
    }

    /**
     * Удаляет карту (только для администраторов).
     *
     * @param cardId идентификатор карты
     * @throws ResourceNotFoundException если карта не найдена
     * @throws ForbiddenException если пользователь не является администратором
     */
    @Transactional
    public void deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Только админ может удалять карты
        if (!currentUser.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"))) {
            throw new ForbiddenException("Only admins can delete cards");
        }

        cardRepository.delete(card);
    }

    /**
     * Получает все карты в системе (только для администраторов).
     *
     * @return список всех карт
     * @throws ForbiddenException если пользователь не является администратором
     */
    @Transactional(readOnly = true)
    public List<CardResponse> getAllCards() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Только админ может видеть все карты
        if (!currentUser.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"))) {
            throw new ForbiddenException("Only admins can view all cards");
        }

        return cardRepository.findAll().stream()
                .map(card -> {
                    String maskedNumber = cardNumberMasker.maskCardNumber(
                            cardNumberEncryptor.decrypt(card.getEncryptedCardNumber()));
                    return CardResponse.fromCard(card, maskedNumber);
                })
                .collect(Collectors.toList());
    }

    /**
     * Находит карту по ID (внутренний метод для использования в других сервисах).
     *
     * @param cardId идентификатор карты
     * @return сущность карты
     * @throws ResourceNotFoundException если карта не найдена
     */
    @Transactional(readOnly = true)
    public Card findCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));
    }
}

