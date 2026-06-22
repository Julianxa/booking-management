package com.example.repository;

import com.example.model.entity.Refunds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface RefundsRepository extends JpaRepository<Refunds, Long> {
    @Query("""
            SELECT r FROM Refunds r
            WHERE r.bookingId = :bookingId
              AND r.status IN ('PENDING', 'PROCESSING', 'SUCCESS')
            """)
    Optional<Refunds> findActiveByBookingId(@Param("bookingId") Long bookingId);

    @Query("""
            SELECT r FROM Refunds r
            WHERE r.bookingId = :bookingId
              AND r.status IN ('PENDING', 'PROCESSING')
            """)
    Optional<Refunds> findByBookingIdAndOngoingStatus(@Param("bookingId") Long bookingId);

    @Query("SELECT r FROM Refunds r WHERE r.bookingId = :bookingId")
    Optional<List<Refunds>> findByBookingId(@Param("bookingId") Long bookingId);
}
