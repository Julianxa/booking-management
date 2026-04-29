package com.example.repository;


import com.example.model.entity.Bookings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BookingsRepository extends JpaRepository<Bookings, Long> {
    boolean existsByRefNo(String refNo);

    @Query("SELECT b.id FROM Bookings b WHERE b.refNo = :refNo")
    Optional<Long> findIdByRefNo(String refNo);

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
}
