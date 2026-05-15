package com.example.repository;

import com.example.model.entity.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentsRepository extends JpaRepository<Payments, String> {
    Optional<Payments> findByBookingId(Long bookingId);

    Optional<Payments> findBySessionId(String sessionId);

    Optional<Payments> findByPaymentIntentId(String paymentIntentId);
}
