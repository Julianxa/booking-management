package com.example.service;

import com.example.constant.Enums;
import com.example.model.entity.BookingEvents;
import com.example.model.entity.Bookings;
import com.example.model.entity.Payments;
import com.example.repository.BookingEventsRepository;
import com.example.repository.BookingsRepository;
import com.example.repository.PaymentsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.constant.Enums.BookingEventStatus.CANCELLED;
import static com.example.constant.Enums.BookingStatus.AWAITING_PAYMENT;
import static com.example.constant.Enums.BookingStatus.CONFIRMED;
import static com.example.constant.Enums.BookingStatus.ON_HOLD;
import static com.example.constant.Enums.BookingStatus.PAID;
import static com.example.constant.Enums.BookingStatus.PAYMENT_IN_PROGRESS;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationService {
    public static final List<Enums.BookingStatus> ACTIVE_PARENT_STATUSES = List.of(
            ON_HOLD, AWAITING_PAYMENT, PAYMENT_IN_PROGRESS, PAID, CONFIRMED);

    public static final List<Enums.BookingStatus> RESTORABLE_PARENT_STATUSES = List.of(
            Enums.BookingStatus.CANCELLED, PAID, CONFIRMED);

    private final BookingEventsRepository bookingEventsRepository;
    private final BookingsRepository bookingsRepository;
    private final PaymentsRepository paymentsRepository;
    private final PaymentLogService paymentLogService;
    private final GiftCertificateService giftCertificateService;
    private final EventSlotReservationService eventSlotReservationService;

    @Transactional
    public void cancelActiveBookingEvents(Bookings booking, boolean releaseCapacity) {
        if (booking == null || booking.getId() == null) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now();
        List<BookingEvents> bookingEvents = bookingEventsRepository.findByBookingId(booking.getId());
        for (BookingEvents bookingEvent : bookingEvents) {
            if (isCancelled(bookingEvent)) {
                continue;
            }
            if (releaseCapacity) {
                eventSlotReservationService.releaseCapacityForBookingEvent(bookingEvent);
            }
            bookingEvent.setStatus(CANCELLED);
            bookingEvent.setCancelledAt(now);
            bookingEvent.setUpdatedAt(now);
            bookingEventsRepository.save(bookingEvent);
        }
        booking.setSlotCapacityHeld(false);
        bookingsRepository.save(booking);
    }

    @Transactional
    public void markBookingCancelledIfNoActiveEvents(Bookings booking) {
        if (booking == null || booking.getId() == null) {
            return;
        }
        Enums.BookingStatus status = booking.getStatus();
        if (status == Enums.BookingStatus.CANCELLED
                || status == Enums.BookingStatus.REFUNDED
                || status == Enums.BookingStatus.EXPIRED
                || status == Enums.BookingStatus.FAILED) {
            return;
        }

        boolean hasActiveEvent = bookingEventsRepository.findByBookingId(booking.getId()).stream()
                .anyMatch(event -> !isCancelled(event));
        if (hasActiveEvent) {
            return;
        }

        if (isUnpaidInProgress(status)) {
            finalizeUnpaidParent(booking);
            return;
        }

        booking.setStatus(Enums.BookingStatus.CANCELLED);
        booking.setSlotCapacityHeld(false);
        booking.setUpdatedAt(ZonedDateTime.now());
        bookingsRepository.save(booking);
        log.info("Marked booking {} as CANCELLED because no active booked events remain", booking.getRefNo());
    }

    public boolean isRestorable(Bookings booking) {
        if (booking == null || booking.getStatus() == null) {
            return false;
        }
        if (booking.getStatus() == PAID || booking.getStatus() == CONFIRMED) {
            return true;
        }
        return booking.getStatus() == Enums.BookingStatus.CANCELLED && wasPaid(booking);
    }

    public List<BookingEvents> keepRestorable(List<BookingEvents> bookingEvents) {
        if (bookingEvents == null || bookingEvents.isEmpty()) {
            return List.of();
        }
        return bookingEvents.stream()
                .filter(event -> isRestorable(event.getBooking()))
                .toList();
    }

    @Transactional
    public void restoreBookingIfCancelled(Bookings booking) {
        if (booking == null || booking.getStatus() != Enums.BookingStatus.CANCELLED || !wasPaid(booking)) {
            return;
        }
        booking.setStatus(CONFIRMED);
        booking.setSlotCapacityHeld(true);
        booking.setUpdatedAt(ZonedDateTime.now());
        bookingsRepository.save(booking);
        log.info("Restored booking {} from CANCELLED to CONFIRMED after booked event restore", booking.getRefNo());
    }

    @Transactional
    public void syncBookingsAfterBulkEventCancel(List<BookingEvents> cancelledEvents) {
        Set<Long> bookingIds = new HashSet<>();
        for (BookingEvents event : cancelledEvents) {
            if (event.getBooking() != null && event.getBooking().getId() != null) {
                bookingIds.add(event.getBooking().getId());
            }
        }
        for (Long bookingId : bookingIds) {
            bookingsRepository.findById(bookingId).ifPresent(this::markBookingCancelledIfNoActiveEvents);
        }
    }

    @Transactional
    public void syncBookingsAfterBulkEventRestore(List<BookingEvents> restoredEvents) {
        Set<Long> bookingIds = new HashSet<>();
        for (BookingEvents event : restoredEvents) {
            if (event.getBooking() != null && event.getBooking().getId() != null) {
                bookingIds.add(event.getBooking().getId());
            }
        }
        for (Long bookingId : bookingIds) {
            bookingsRepository.findById(bookingId).ifPresent(this::restoreBookingIfCancelled);
        }
    }

    private static boolean isCancelled(BookingEvents bookingEvent) {
        return bookingEvent.getStatus() == CANCELLED || bookingEvent.getCancelledAt() != null;
    }

    private static boolean isUnpaidInProgress(Enums.BookingStatus status) {
        return status == ON_HOLD
                || status == AWAITING_PAYMENT
                || status == PAYMENT_IN_PROGRESS;
    }

    private void finalizeUnpaidParent(Bookings booking) {
        giftCertificateService.cancelCertificateRedemption(booking);

        Payments payment = paymentsRepository.findByBookingId(booking.getId()).orElse(null);
        if (paymentLogService.hasFailedAttempt(payment)) {
            booking.setStatus(Enums.BookingStatus.FAILED);
            log.info("Marked unpaid booking {} as FAILED after admin close with no remaining events", booking.getRefNo());
        } else {
            booking.setStatus(Enums.BookingStatus.EXPIRED);
            if (payment != null && payment.getPaymentStatus() != Enums.PaymentStatus.SUCCEEDED) {
                payment.setPaymentStatus(Enums.PaymentStatus.EXPIRED);
                paymentsRepository.save(payment);
                paymentLogService.recordStatusChange(payment, Enums.PaymentStatus.EXPIRED, null, null);
            }
            log.info("Marked unpaid booking {} as EXPIRED after admin close with no remaining events", booking.getRefNo());
        }

        booking.setSlotCapacityHeld(false);
        booking.setUpdatedAt(ZonedDateTime.now());
        bookingsRepository.save(booking);
    }

    private boolean wasPaid(Bookings booking) {
        if (booking.getType() == Enums.BookingType.OFFLINE_PAYMENT) {
            return true;
        }
        if (booking.getId() == null) {
            return false;
        }
        return paymentsRepository
                .findByBookingIdAndPaymentStatus(booking.getId(), Enums.PaymentStatus.SUCCEEDED)
                .isPresent();
    }
}
