package com.example.scheduler;

import com.example.constant.Enums;
import com.example.model.entity.Bookings;
import com.example.repository.BookingsRepository;
import com.example.service.EventSlotReservationService;
import com.example.service.GiftCertificateService;
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
    private final GiftCertificateService giftCertificateService;
    private final EventSlotReservationService eventSlotReservationService;

    @Value("${app.booking.cleanup.pending-timeout-minutes:5}")
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
                Enums.BookingStatus.PENDING,
                Enums.BookingStatus.FAILED,
                now.minusMinutes(pendingTimeoutMinutes));
        releasedCount += releaseStaleBookings(
                Enums.BookingStatus.AWAITING_PAYMENT,
                Enums.BookingStatus.EXPIRED,
                now.minusMinutes(awaitingPaymentTimeoutMinutes));
        releasedCount += releaseStaleBookings(
                Enums.BookingStatus.PAYMENT_IN_PROGRESS,
                Enums.BookingStatus.EXPIRED,
                now.minusMinutes(paymentInProgressTimeoutMinutes));

        if (releasedCount > 0) {
            log.info("Released {} stale booking reservation(s)", releasedCount);
        }
    }

    private int releaseStaleBookings(Enums.BookingStatus staleStatus,
                                     Enums.BookingStatus terminalStatus,
                                     ZonedDateTime cutoff) {
        List<Bookings> staleBookings = bookingsRepository.findStaleBookingsForUpdate(
                staleStatus,
                cutoff,
                PageRequest.of(0, batchSize));

        for (Bookings booking : staleBookings) {
            eventSlotReservationService.releaseCapacityForBooking(booking);
            giftCertificateService.cancelCertificateRedemption(booking);
            booking.setStatus(terminalStatus);
            bookingsRepository.save(booking);
            log.warn("Released stale {} booking reservation {}", staleStatus, booking.getRefNo());
        }

        return staleBookings.size();
    }
}
