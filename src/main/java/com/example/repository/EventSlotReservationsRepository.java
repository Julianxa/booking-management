package com.example.repository;

import com.example.model.entity.EventSlotReservationId;
import com.example.model.entity.EventSlotReservations;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface EventSlotReservationsRepository extends JpaRepository<EventSlotReservations, EventSlotReservationId> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO event_slot_reservations (event_id, event_date, event_time, max_capacity, reserved_qty, version)
            VALUES (:eventId, :eventDate, :eventTime, :maxCapacity, 0, 0)
            ON DUPLICATE KEY UPDATE event_id = event_id
            """, nativeQuery = true)
    void ensureSlotExists(
            @Param("eventId") Long eventId,
            @Param("eventDate") LocalDate eventDate,
            @Param("eventTime") String eventTime,
            @Param("maxCapacity") int maxCapacity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE event_slot_reservations
            SET reserved_qty = reserved_qty + :qty,
                version = version + 1
            WHERE event_id = :eventId
              AND event_date = :eventDate
              AND event_time = :eventTime
              AND reserved_qty + :qty <= max_capacity
            """, nativeQuery = true)
    int tryReserveCapacity(
            @Param("eventId") Long eventId,
            @Param("eventDate") LocalDate eventDate,
            @Param("eventTime") String eventTime,
            @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE event_slot_reservations
            SET reserved_qty = GREATEST(0, reserved_qty - :qty),
                version = version + 1
            WHERE event_id = :eventId
              AND event_date = :eventDate
              AND event_time = :eventTime
            """, nativeQuery = true)
    int releaseCapacity(
            @Param("eventId") Long eventId,
            @Param("eventDate") LocalDate eventDate,
            @Param("eventTime") String eventTime,
            @Param("qty") int qty);
}
