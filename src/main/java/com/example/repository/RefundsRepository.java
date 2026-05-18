package com.example.repository;

import com.example.model.entity.Refunds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface RefundsRepository extends JpaRepository<Refunds, String>  {
    @Query("SELECT r FROM Refunds r WHERE r.bookingId = :bookingId and (status = 'SUCCESS' or status = 'PROCESSING')")
    Refunds findByBookingIdAndSuccessOrProcessingStatus(Long bookingId);

    @Query("SELECT r FROM Refunds r WHERE r.bookingId = :bookingId and status != 'SUCCESS' and status != 'FAILED'")
    Refunds findByBookingIdAndOngoingStatus(Long bookingId);
}
