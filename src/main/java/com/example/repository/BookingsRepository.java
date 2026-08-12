package com.example.repository;


import com.example.constant.Enums;
import com.example.model.entity.Bookings;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingsRepository extends JpaRepository<Bookings, Long> {
    boolean existsByRefNo(String refNo);

    @Query("SELECT b.id FROM Bookings b WHERE b.refNo = :refNo")
    Optional<Long> findIdByRefNo(String refNo);

    @Query("SELECT b.refNo FROM Bookings b WHERE b.id = :id")
    Optional<String> findRefNoById(Long id);

    Page<Bookings> findByUserId(Long userId, Pageable pageable);

    Optional<Bookings> findByRefNo(String refNo);
    
    @Query(value = """
            SELECT b.*
            FROM bookings b
            JOIN booking_events be ON be.booking_id = b.id
            WHERE be.event_id = :eventId
            ORDER BY b.created_at DESC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT b.id)
                    FROM bookings b
                    JOIN booking_events be ON be.booking_id = b.id
                    WHERE be.event_id = :eventId
                    """, nativeQuery = true)
    Page<Bookings> findBookingsByEventId(@Param("eventId") Long eventId, Pageable pageable);

    @Query(
            value = """
            SELECT DISTINCT b.*
            FROM bookings b
            JOIN booking_events be ON be.booking_id = b.id
            JOIN booking_attendees ba ON ba.booking_event_id = be.id
            WHERE ba.deleted_at IS NULL
              AND (
                (:field = 'name' AND (
                    LOWER(ba.first_name) LIKE LOWER(CONCAT('%', :value, '%'))
                    OR LOWER(ba.last_name) LIKE LOWER(CONCAT('%', :value, '%'))
                    OR LOWER(CONCAT(ba.first_name, ' ', ba.last_name)) LIKE LOWER(CONCAT('%', :value, '%'))
                ))
                OR (:field = 'email' AND LOWER(ba.email) LIKE LOWER(CONCAT('%', :value, '%')))
                OR (:field = 'phone' AND ba.phone LIKE CONCAT('%', :value, '%'))
              )
            ORDER BY b.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT b.id)
            FROM bookings b
            JOIN booking_events be ON be.booking_id = b.id
            JOIN booking_attendees ba ON ba.booking_event_id = be.id
            WHERE ba.deleted_at IS NULL
              AND (
                (:field = 'name' AND (
                    LOWER(ba.first_name) LIKE LOWER(CONCAT('%', :value, '%'))
                    OR LOWER(ba.last_name) LIKE LOWER(CONCAT('%', :value, '%'))
                    OR LOWER(CONCAT(ba.first_name, ' ', ba.last_name)) LIKE LOWER(CONCAT('%', :value, '%'))
                ))
                OR (:field = 'email' AND LOWER(ba.email) LIKE LOWER(CONCAT('%', :value, '%')))
                OR (:field = 'phone' AND ba.phone LIKE CONCAT('%', :value, '%'))
              )
            """,
            nativeQuery = true)
    Page<Bookings> searchByAttendeeField(
            @Param("field") String field, @Param("value") String value, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bookings b WHERE b.id = :id")
    Optional<Bookings> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Bookings b
            WHERE b.status = :status
              AND b.updatedAt < :cutoff
            ORDER BY b.updatedAt ASC
            """)
    List<Bookings> findStaleBookingsForUpdate(
            @Param("status") Enums.BookingStatus status,
            @Param("cutoff") ZonedDateTime cutoff,
            Pageable pageable);
}
