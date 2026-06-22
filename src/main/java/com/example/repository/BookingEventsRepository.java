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
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface BookingEventsRepository extends JpaRepository<BookingEvents, Long> {
    boolean existsByRefNo(String refNo);

    @Query("SELECT be.id FROM BookingEvents be WHERE be.refNo = :refNo")
    Optional<Long> findIdByRefNo(String refNo);

    @Query(value = "SELECT id FROM booking_events WHERE booking_id = :bookingId AND event_id = :eventId", nativeQuery = true)
    Optional<Long> findIdByBookingIdAndEventId(Long bookingId, Long eventId);

    List<BookingEvents> findByBookingId(Long bookingId);

    Optional<BookingEvents> findByRefNo(String bookingEventId);

    Optional<BookingEvents> findByVerificationToken(String verificationToken);

    BookingEvents findByBooking_RefNoAndEvent_RefNoAndEventDateAndEventTime(String bookingRefNo, String eventRefNo, LocalDate eventDate, String eventTime);

    @Query("""
    SELECT be FROM BookingEvents be
    JOIN FETCH be.booking b
    JOIN FETCH be.event e
    WHERE e.refNo = :eventRefNo
      AND be.eventDate = :eventDate
      AND be.eventTime = :eventTime
      AND be.cancelledAt IS NULL
      AND be.status IN ('AVAILABLE')
    """)
    List<BookingEvents> findActiveByEventRefNoAndEventDateAndEventTime(
            @Param("eventRefNo") String eventRefNo,
            @Param("eventDate") LocalDate eventDate,
            @Param("eventTime") String eventTime);

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
              AND status = :currentStatus
            """, nativeQuery = true)
    void updateCancelStatusBookingsByEventTimeSlot(
            @Param("eventId") Long eventId,
            @Param("eventDate") LocalDate eventDate,
            @Param("eventTime") String eventTime,
            @Param("currentStatus") String currentStatus,
            @Param("status") String status,
            @Param("cancelledAt") ZonedDateTime cancelledAt);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE booking_events
            SET status = :status,
                cancelled_at = :cancelledAt,
                updated_at = CURRENT_TIMESTAMP
            WHERE event_id = :eventId
              AND status = :currentStatus
            """, nativeQuery = true)
    void updateCancelStatusBookingsByEventId(
            @Param("eventId") Long eventId,
            @Param("currentStatus") String currentStatus,
            @Param("status") String status,
            @Param("cancelledAt") ZonedDateTime cancelledAt);

    @Query(value = """
            SELECT be.id
            FROM booking_events be
            WHERE be.event_date = :targetDate
              AND be.status = 'AVAILABLE'
              AND be.reminder_sent_at IS NULL
              AND be.cancelled_at IS NULL
            ORDER BY be.id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Long> findReminderCandidateIdsForUpdateSkipLocked(
            @Param("targetDate") LocalDate targetDate,
            @Param("limit") int limit);

    @Modifying
    @Query("""
            UPDATE BookingEvents be
            SET be.reminderSentAt = :claimedAt
            WHERE be.id IN :ids AND be.reminderSentAt IS NULL
            """)
    int markReminderClaimed(@Param("ids") List<Long> ids, @Param("claimedAt") ZonedDateTime claimedAt);

    @Modifying
    @Query("""
            UPDATE BookingEvents be
            SET be.reminderSentAt = NULL
            WHERE be.id = :id AND be.reminderSentAt = :claimedAt
            """)
    int releaseReminderClaim(@Param("id") Long id, @Param("claimedAt") ZonedDateTime claimedAt);

    @Modifying
    @Query("""
            UPDATE BookingEvents be
            SET be.reminderSentAt = :sentAt
            WHERE be.id = :id AND be.reminderSentAt = :claimedAt
            """)
    int markReminderSentFromClaim(
            @Param("id") Long id,
            @Param("claimedAt") ZonedDateTime claimedAt,
            @Param("sentAt") ZonedDateTime sentAt);

    @Query("""
            SELECT be FROM BookingEvents be
            JOIN FETCH be.booking
            JOIN FETCH be.event
            WHERE be.id = :id
            """)
    Optional<BookingEvents> findByIdWithBookingAndEvent(@Param("id") Long id);
}
