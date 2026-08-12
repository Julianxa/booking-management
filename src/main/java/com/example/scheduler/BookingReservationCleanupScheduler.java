package com.example.scheduler;

import com.example.constant.Enums;
import com.example.model.entity.Bookings;
import com.example.model.entity.Payments;
import com.example.repository.OctoBookingMappingsRepository;
import com.example.repository.BookingsRepository;
import com.example.repository.PaymentsRepository;
import com.example.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReservationCleanupScheduler {
    private final BookingsRepository bookingsRepository;
    private final PaymentsRepository paymentsRepository;
    private final WebhookService webhookService;
    private final OctoBookingMappingsRepository octoBookingMappingsRepository;

    @Value("${app.booking.cleanup.pending-timeout-minutes:10}")
    private long pendingTimeoutMinutes;

    @Value("${app.booking.cleanup.awaiting-payment-timeout-minutes:35}")
    private long awaitingPaymentTimeoutMinutes;

    @Value("${app.booking.cleanup.payment-in-progress-timeout-minutes:60}")
    private long paymentInProgressTimeoutMinutes;

    @Value("${app.booking.cleanup.batch-size:100}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${app.booking.cleanup.fixed-delay-ms:60000}",
            initialDelayString = "${app.booking.cleanup.initial-delay-ms:60000}"
    )
    @Transactional
    public void releaseStaleReservations() {
        ZonedDateTime now = ZonedDateTime.now();

        int releasedCount = 0;
        releasedCount += releaseStaleBookings(
                Enums.BookingStatus.ON_HOLD,
                now.minusMinutes(pendingTimeoutMinutes));
        releasedCount += releaseStaleBookings(
                Enums.BookingStatus.AWAITING_PAYMENT,
                now.minusMinutes(awaitingPaymentTimeoutMinutes));
        releasedCount += releaseStaleBookings(
                Enums.BookingStatus.PAYMENT_IN_PROGRESS,
                now.minusMinutes(paymentInProgressTimeoutMinutes));

        if (releasedCount > 0) {
            log.info("Released {} stale booking reservation(s)", releasedCount);
        }
    }

    private int releaseStaleBookings(Enums.BookingStatus staleStatus, ZonedDateTime cutoff) {
        List<Bookings> staleBookings = bookingsRepository.findStaleBookingsForUpdate(
                staleStatus,
                cutoff,
                PageRequest.of(0, batchSize));

        int released = 0;
        for (Bookings booking : staleBookings) {
            Payments payment = paymentsRepository.findByBookingId(booking.getId()).orElse(null);
            webhookService.finalizeUnpaidBooking(booking, payment, "scheduler:" + staleStatus);
            markOctoMappingExpiredIfPresent(booking.getId());
            log.warn("Released stale {} booking reservation {}", staleStatus, booking.getRefNo());
            released++;
        }

        return released;
    }

    private void markOctoMappingExpiredIfPresent(Long bookingId) {
        octoBookingMappingsRepository
                .findByBookingId(bookingId)
                .ifPresent(
                        mapping -> {
                            if ("ON_HOLD".equals(mapping.getOctoStatus())) {
                                mapping.setOctoStatus("EXPIRED");
                                mapping.setHoldExpiresAt(null);
                                octoBookingMappingsRepository.save(mapping);
                            }
                        });
    }
}
