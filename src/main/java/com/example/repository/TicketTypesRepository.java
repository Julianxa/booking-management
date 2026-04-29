package com.example.repository;

import com.example.constant.Enums;
import com.example.model.entity.TicketTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketTypesRepository extends JpaRepository<TicketTypes, Long> {
    Optional<TicketTypes> findByRefNo(String ticketTypeRefNo);

    @Query("SELECT tt.id FROM TicketTypes tt WHERE tt.refNo = :refNo")
    Optional<Long> findIdByRefNo(String refNo);
    boolean existsByRefNo(String refNo);
    @Query("SELECT tt.refNo FROM TicketTypes tt WHERE tt.id = :id")
    Optional<String> findRefNoById(Long id);

    @Query(value="""
            SELECT * FROM ticket_types tt
            WHERE tt.event_id = :eventId AND tt.status != 'CLOSE'
            """, nativeQuery = true)
    List<TicketTypes> findByEventId(Long eventId);

    @Modifying
    @Transactional
    @Query("""
    UPDATE TicketTypes t
    SET t.status = :status,
        t.updatedAt = :deletedAt,
        t.deletedAt = :deletedAt
    WHERE t.event.id = :eventId
      AND t.refNo = :ticketTypeRefNo
    """)
    void updateDeleteStatusByEventIdAndTicketTypesRefNo(Long eventId, String ticketTypeRefNo, Enums.TicketTypeStatus status, LocalDateTime deletedAt);

    @Modifying
    @Transactional
    @Query("""
    UPDATE TicketTypes t
    SET t.status = :status,
        t.updatedAt = :updatedAt,
        t.deletedAt = null
    WHERE t.event.id = :eventId
      AND t.refNo = :ticketTypeRefNo
    """)
    void updateOpenStatusByEventIdAndTicketTypesRefNo(Long eventId, String ticketTypeRefNo, Enums.TicketTypeStatus status, LocalDateTime updatedAt);
}
