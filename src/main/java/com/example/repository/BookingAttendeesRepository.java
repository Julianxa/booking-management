package com.example.repository;

import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.BookingAttendees;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingAttendeesRepository extends JpaRepository<BookingAttendees, Long> {

    @Query(value = """
    SELECT
        ba.first_name AS first_name,
        ba.last_name AS last_name,
        ba.email AS email,
        ba.phone AS phone,
        ba.gender AS gender,
        ba.country AS country,
        ba.sequence AS sequence
    FROM booking_attendees ba
    INNER JOIN booking_events be ON ba.booking_event_id = be.id
    WHERE be.booking_id = :bookingId
    ORDER BY ba.sequence ASC, ba.id ASC
    """, nativeQuery = true)
    List<CreateBookingRequestDTO.AttendeeDTO> findAttendeesByBookingId(@Param("bookingId") Long bookingId);

    @Query(value = """
    SELECT
        ba.first_name AS first_name,
        ba.last_name AS last_name,
        ba.email AS email,
        ba.phone AS phone,
        ba.gender AS gender,
        ba.country AS country,
        ba.sequence AS sequence
    FROM booking_attendees ba
    INNER JOIN booking_events be ON ba.booking_event_id = be.id
    WHERE be.id = :bookingEventId
    ORDER BY ba.sequence ASC, ba.id ASC
    """, nativeQuery = true)
    List<CreateBookingRequestDTO.AttendeeDTO> findAttendeesByBookingEventId(@Param("bookingEventId") Long bookingEventId);

    void deleteByBookingEventId(Long bookingEventId);
}
