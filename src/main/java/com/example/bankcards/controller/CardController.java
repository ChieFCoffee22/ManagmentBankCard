package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CardStatusUpdateRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для управления банковскими картами.
 * Обрабатывает CRUD операции с картами и переводы между картами.
 * 
 * @author system
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;
    private final TransferService transferService;
    private final UserRepository userRepository;

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param cardService сервис для работы с картами
     * @param transferService сервис для переводов между картами
     * @param userRepository репозиторий пользователей
     */
    public CardController(CardService cardService,
                          TransferService transferService,
                          UserRepository userRepository) {
        this.cardService = cardService;
        this.transferService = transferService;
        this.userRepository = userRepository;
    }

    /**
     * Получает список карт пользователя с фильтрацией и пагинацией.
     *
     * @param userId ID пользователя (опционально, по умолчанию текущий пользователь)
     * @param cardholderName фильтр по имени держателя карты (опционально)
     * @param status фильтр по статусу карты (опционально)
     * @param page номер страницы (по умолчанию 0)
     * @param size размер страницы (по умолчанию 10)
     * @param sortBy поле для сортировки (по умолчанию "id")
     * @param sortDir направление сортировки (ASC или DESC, по умолчанию DESC)
     * @return страница с картами пользователя
     */
    @GetMapping
    public ResponseEntity<Page<CardResponse>> getMyCards(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String cardholderName,
            @RequestParam(required = false) Card.CardStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Если userId не указан, используем ID текущего пользователя
        Long targetUserId = userId != null ? userId : currentUser.getId();

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CardResponse> cards = cardService.getAllCardsForUser(targetUserId, cardholderName, status, pageable);
        return ResponseEntity.ok(cards);
    }

    /**
     * Получает карту по ID.
     *
     * @param id идентификатор карты
     * @return информация о карте
     */
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable Long id) {
        CardResponse card = cardService.getCardById(id);
        return ResponseEntity.ok(card);
    }

    /**
     * Создает новую банковскую карту.
     *
     * @param request данные для создания карты
     * @return созданная карта
     */
    @PostMapping
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardCreateRequest request) {
        CardResponse card = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    /**
     * Обновляет статус карты.
     *
     * @param id идентификатор карты
     * @param request новый статус карты
     * @return обновленная карта
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<CardResponse> updateCardStatus(
            @PathVariable Long id,
            @Valid @RequestBody CardStatusUpdateRequest request) {
        CardResponse card = cardService.updateCardStatus(id, request);
        return ResponseEntity.ok(card);
    }

    /**
     * Удаляет карту (только для администраторов).
     *
     * @param id идентификатор карты
     * @return пустой ответ со статусом 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Выполняет перевод между картами текущего пользователя.
     *
     * @param request данные перевода (fromCardId, toCardId, amount)
     * @return результат перевода
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transferBetweenCards(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transferBetweenOwnCards(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Получает все карты в системе (только для администраторов).
     *
     * @return список всех карт
     */
    @GetMapping("/all")
    public ResponseEntity<List<CardResponse>> getAllCards() {
        List<CardResponse> cards = cardService.getAllCards();
        return ResponseEntity.ok(cards);
    }
}

