package com.example.repository;

import com.example.model.entity.TicketPricePeriods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TicketPricePeriodsRepository extends JpaRepository<TicketPricePeriods, Long> {
    @Query("""
    SELECT p FROM TicketPricePeriods p
    WHERE p.ticketTypes.id = :ticketTypeId
      AND (
            :currentTime IS NULL
            OR (
                (p.effectiveFrom IS NULL OR p.effectiveFrom <= :currentTime)
                AND (p.effectiveTo IS NULL OR p.effectiveTo >= :currentTime)
            )
          )
    ORDER BY p.effectiveFrom DESC
    """)
    Optional<TicketPricePeriods> findActivePrice(
            @Param("ticketTypeId") Long ticketTypeId,
            @Param("currentTime") LocalDateTime currentTime);
}
