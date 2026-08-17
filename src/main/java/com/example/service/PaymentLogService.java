package com.example.service;

import com.example.constant.Enums;
import com.example.model.entity.PaymentLogs;
import com.example.model.entity.Payments;
import com.example.repository.PaymentLogRepository;
import com.example.utils.ReferenceNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentLogService {
    private final PaymentLogRepository paymentLogRepository;
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

        PaymentLogs logEntry = PaymentLogs.builder()
                .refNo(referenceNoGenerator.generatePaymentLogReference())
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .sessionId(payment.getSessionId())
                .paymentIntentId(payment.getPaymentIntentId())
                .paymentMethod(resolvePaymentMethod(payment, paymentMethod))
                .paymentStatus(paymentStatus)
                .failureReason(failureReason)
                .build();

        paymentLogRepository.save(logEntry);
        log.debug("Recorded payment log {} for payment {} status {}",
                logEntry.getRefNo(), payment.getRefNo(), paymentStatus);
    }

    public boolean hasFailedAttempt(Payments payment) {
        if (payment == null) {
            return false;
        }
        if (payment.getPaymentStatus() == Enums.PaymentStatus.FAILED) {
            return true;
        }
        return payment.getId() != null
                && paymentLogRepository.existsByPaymentIdAndPaymentStatus(payment.getId(), Enums.PaymentStatus.FAILED);
    }

    private String resolvePaymentMethod(Payments payment, String paymentMethod) {
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            return paymentMethod;
        }
        if (payment.getPaymentChannel() != null) {
            return payment.getPaymentChannel().name().toLowerCase();
        }
        return null;
    }
}
