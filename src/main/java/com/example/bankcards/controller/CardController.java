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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private UserRepository userRepository;

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

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable Long id) {
        CardResponse card = cardService.getCardById(id);
        return ResponseEntity.ok(card);
    }

    @PostMapping
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardCreateRequest request) {
        CardResponse card = cardService.createCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CardResponse> updateCardStatus(
            @PathVariable Long id,
            @Valid @RequestBody CardStatusUpdateRequest request) {
        CardResponse card = cardService.updateCardStatus(id, request);
        return ResponseEntity.ok(card);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transferBetweenCards(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferService.transferBetweenOwnCards(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CardResponse>> getAllCards() {
        List<CardResponse> cards = cardService.getAllCards();
        return ResponseEntity.ok(cards);
    }
}

