package com.example.repository;


import com.example.model.entity.BookingAttendees;
import com.example.model.entity.BookingEvents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface BookingEventsRepository extends JpaRepository<BookingEvents, Long> {
    boolean existsByRefNo(String refNo);

    @Query("SELECT be.id FROM BookingEvents be WHERE be.refNo = :refNo")
    Optional<Long> findIdByRefNo(String refNo);

    List<BookingEvents> findByBookingId(Long bookingId);

    Optional<BookingEvents> findByRefNo(String bookingId);

    Optional<BookingEvents> findByVerificationToken(String verificationToken);

    BookingEvents findByBooking_RefNoAndEvent_RefNoAndEventDateAndEventTime(String bookingRefNo, String eventRefNo, LocalDate eventDate, String eventTime);

    @Query(value = """
    SELECT ba.*
    FROM booking_events be
    INNER JOIN booking_attendees ba ON ba.booking_event_id = be.id
    WHERE be.event_id = :eventId
      AND be.event_date = :eventDate
      AND be.event_time = :eventTime
      AND be.cancelled_at IS NULL
      AND be.status IN ('AVAILABLE', 'CHECKED_IN')
    ORDER BY be.id, ba.sequence ASC, ba.id ASC
    """,
            countQuery = """
    SELECT COUNT(ba.id)
    FROM booking_events be
    INNER JOIN booking_attendees ba ON ba.booking_event_id = be.id
    WHERE be.event_id = :eventId
      AND be.event_date = :eventDate
      AND be.event_time = :eventTime
      AND be.cancelled_at IS NULL
      AND be.status IN ('AVAILABLE', 'CHECKED_IN')
    """,
            nativeQuery = true)
    Page<BookingAttendees> findPassengersByEventDateTime(
            @Param("eventId") Long eventId,
            @Param("eventDate") LocalDate eventDate,
            @Param("eventTime") String eventTime,
            Pageable pageable);

    @Modifying
    @Query("""
        UPDATE BookingEvents be
        SET be.notes = :notes
        WHERE be.refNo = :bookingEventRefNo
    """)
    void updateNotes(@Param("bookingEventRefNo") String bookingEventRefNo,
                    @Param("notes") String notes);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE booking_events
        SET status = :status,
            cancelled_at = :cancelledAt,
            updated_at = CURRENT_TIMESTAMP
        WHERE event_id = :eventId 
          AND event_date = :eventDate 
          AND event_time = :eventTime
        """, nativeQuery = true)
    void updateCancelStatusBookingsByEventTimeSlot(
            @Param("eventId") Long eventId,
            @Param("eventDate") LocalDate eventDate,
            @Param("eventTime") String eventTime,
            @Param("status") String status,
            @Param("cancelledAt") LocalDateTime cancelledAt);
}
