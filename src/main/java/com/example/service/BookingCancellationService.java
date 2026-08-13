package com.example.service;

import com.example.constant.Enums;
import com.example.model.entity.BookingEvents;
import com.example.model.entity.Bookings;
import com.example.repository.BookingEventsRepository;
import com.example.repository.BookingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.constant.Enums.BookingEventStatus.CANCELLED;
import static com.example.constant.Enums.BookingStatus.CONFIRMED;

/**
 * Keeps booking-level and booking-event-level cancellation in sync.
 * - Cancelling the last active booked event marks the parent booking CANCELLED.
 * - Whole-booking terminal paths (refund / expire / fail) cancel all active booked events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationService {
    private final BookingEventsRepository bookingEventsRepository;
    private final BookingsRepository bookingsRepository;
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

        booking.setStatus(Enums.BookingStatus.CANCELLED);
        booking.setSlotCapacityHeld(false);
        booking.setUpdatedAt(ZonedDateTime.now());
        bookingsRepository.save(booking);
        log.info("Marked booking {} as CANCELLED because no active booked events remain", booking.getRefNo());
    }

    @Transactional
    public void restoreBookingIfCancelled(Bookings booking) {
        if (booking == null || booking.getStatus() != Enums.BookingStatus.CANCELLED) {
            return;
        }
        booking.setStatus(CONFIRMED);
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
}
