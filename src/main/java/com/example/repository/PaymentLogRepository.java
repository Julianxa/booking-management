package com.example.repository;

import com.example.constant.Enums;
import com.example.model.entity.PaymentLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLogs, Long> {
    boolean existsByRefNo(String refNo);

    boolean existsByPaymentIdAndPaymentStatus(Long paymentId, Enums.PaymentStatus paymentStatus);
}
