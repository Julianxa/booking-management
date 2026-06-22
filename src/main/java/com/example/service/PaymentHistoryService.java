package com.example.service;

import com.example.constant.Enums;
import com.example.model.entity.PaymentHistory;
import com.example.model.entity.Payments;
import com.example.repository.PaymentHistoryRepository;
import com.example.utils.ReferenceNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentHistoryService {
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final ReferenceNoGenerator referenceNoGenerator;

    @Transactional
    public void recordStatusChange(
            Payments payment,
            Enums.PaymentStatus paymentStatus,
            String failureReason,
            String paymentMethod) {
        if (payment == null || paymentStatus == null) {
            return;
        }

        PaymentHistory history = PaymentHistory.builder()
                .refNo(referenceNoGenerator.generatePaymentHistoryReference())
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .sessionId(payment.getSessionId())
                .paymentIntentId(payment.getPaymentIntentId())
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
                .failureReason(failureReason)
                .build();

        paymentHistoryRepository.save(history);
        log.debug("Recorded payment history {} for payment {} status {}",
                history.getRefNo(), payment.getRefNo(), paymentStatus);
    }
}
