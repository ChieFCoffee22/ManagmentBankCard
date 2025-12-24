package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.dto.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardTransaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardTransactionRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardNumberEncryptor;
import com.example.bankcards.util.CardNumberMasker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сервис для выполнения переводов между банковскими картами.
 * Обеспечивает атомарность операций перевода и проверку бизнес-правил.
 * 
 * @author system
 */
@Service
public class TransferService {

    private final CardRepository cardRepository;
    private final CardTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CardNumberEncryptor cardNumberEncryptor;
    private final CardNumberMasker cardNumberMasker;

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param cardRepository репозиторий карт
     * @param transactionRepository репозиторий транзакций
     * @param userRepository репозиторий пользователей
     * @param cardNumberEncryptor утилита для шифрования номеров карт
     * @param cardNumberMasker утилита для маскирования номеров карт
     */
    public TransferService(CardRepository cardRepository,
                          CardTransactionRepository transactionRepository,
                          UserRepository userRepository,
                          CardNumberEncryptor cardNumberEncryptor,
                          CardNumberMasker cardNumberMasker) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.cardNumberEncryptor = cardNumberEncryptor;
        this.cardNumberMasker = cardNumberMasker;
    }

    /**
     * Выполняет перевод средств между картами одного пользователя.
     * Операция атомарна - либо выполняется полностью, либо откатывается.
     * 
     * Проверяет:
     * - принадлежность обеих карт текущему пользователю
     * - что карты не одинаковые
     * - статус карт (должны быть ACTIVE)
     * - срок действия карт
     * - достаточность баланса на карте-источнике
     *
     * @param request данные перевода (fromCardId, toCardId, amount)
     * @return результат перевода с информацией о транзакции
     * @throws ForbiddenException если карты не принадлежат текущему пользователю
     * @throws BadRequestException если нарушены бизнес-правила (недостаточно средств, карта неактивна и т.д.)
     */
    @Transactional
    public TransferResponse transferBetweenOwnCards(TransferRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Card fromCard = cardRepository.findById(request.getFromCardId())
                .orElseThrow(() -> new ResourceNotFoundException("From card not found with id: " + request.getFromCardId()));

        Card toCard = cardRepository.findById(request.getToCardId())
                .orElseThrow(() -> new ResourceNotFoundException("To card not found with id: " + request.getToCardId()));

        // Проверка, что обе карты принадлежат текущему пользователю
        if (!fromCard.getOwner().getId().equals(currentUser.getId()) || 
            !toCard.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only transfer between your own cards");
        }

        // Проверка, что карты не одинаковые
        if (fromCard.getId().equals(toCard.getId())) {
            throw new BadRequestException("Cannot transfer to the same card");
        }

        // Проверка статуса карт
        if (fromCard.getStatus() != Card.CardStatus.ACTIVE) {
            throw new BadRequestException("From card is not active");
        }

        if (toCard.getStatus() != Card.CardStatus.ACTIVE) {
            throw new BadRequestException("To card is not active");
        }

        // Проверка срока действия
        if (fromCard.isExpired()) {
            throw new BadRequestException("From card has expired");
        }

        if (toCard.isExpired()) {
            throw new BadRequestException("To card has expired");
        }

        // Проверка баланса
        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient funds");
        }

        // Выполнение перевода
        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        // Создание записи о транзакции
        CardTransaction transaction = new CardTransaction();
        transaction.setFromCard(fromCard);
        transaction.setToCard(toCard);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(LocalDateTime.now());

        CardTransaction savedTransaction = transactionRepository.save(transaction);

        // Формирование ответа
        TransferResponse response = new TransferResponse();
        response.setId(savedTransaction.getId());
        response.setFromCardId(fromCard.getId());
        response.setFromCardMaskedNumber(cardNumberMasker.maskCardNumber(
                cardNumberEncryptor.decrypt(fromCard.getEncryptedCardNumber())));
        response.setToCardId(toCard.getId());
        response.setToCardMaskedNumber(cardNumberMasker.maskCardNumber(
                cardNumberEncryptor.decrypt(toCard.getEncryptedCardNumber())));
        response.setAmount(request.getAmount());
        response.setTransactionDate(savedTransaction.getTransactionDate());
        response.setMessage("Transfer completed successfully");

        return response;
    }
}

