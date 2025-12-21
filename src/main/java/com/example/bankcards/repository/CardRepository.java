package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Page<Card> findByOwnerId(Long ownerId, Pageable pageable);
    
    @Query("SELECT c FROM Card c WHERE c.owner.id = :ownerId " +
           "AND (:cardholderName IS NULL OR LOWER(c.cardholderName) LIKE LOWER(CONCAT('%', :cardholderName, '%'))) " +
           "AND (:status IS NULL OR c.status = :status)")
    Page<Card> findByOwnerIdWithFilters(
            @Param("ownerId") Long ownerId,
            @Param("cardholderName") String cardholderName,
            @Param("status") Card.CardStatus status,
            Pageable pageable
    );
    
    List<Card> findByOwnerId(Long ownerId);
    
    Optional<Card> findByIdAndOwnerId(Long id, Long ownerId);
    
    @Query("SELECT c FROM Card c WHERE c.owner.id = :ownerId AND c.status = 'ACTIVE'")
    List<Card> findActiveCardsByOwnerId(@Param("ownerId") Long ownerId);
}

