package com.example.repository;

import com.example.model.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    boolean existsByRefNo(String refNo);

    List<PaymentHistory> findByPaymentIdOrderByCreatedAtAsc(Long paymentId);

    List<PaymentHistory> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
}
