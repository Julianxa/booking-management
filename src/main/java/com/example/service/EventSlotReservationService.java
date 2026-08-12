package com.example.service;

import com.example.constant.Enums;
import com.example.exception.event.EventCapacityExceededException;
import com.example.model.entity.BookingEvents;
import com.example.model.entity.Bookings;
import com.example.model.entity.EventSlotReservationId;
import com.example.model.entity.EventSlotReservations;
import com.example.model.entity.Events;
import com.example.repository.BookingEventsRepository;
import com.example.repository.BookingItemsRepository;
import com.example.repository.BookingsRepository;
import com.example.repository.EventSlotReservationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSlotReservationService {
    private final EventSlotReservationsRepository eventSlotReservationsRepository;
    private final BookingEventsRepository bookingEventsRepository;
    private final BookingItemsRepository bookingItemsRepository;
    private final BookingsRepository bookingsRepository;

    @Transactional
    public void reserveCapacity(
            Long eventId,
            LocalDate eventDate,
            String eventTime,
            int qty,
            Integer maxCapacity,
            String eventName) {
        if (qty <= 0) {
            return;
        }

        eventSlotReservationsRepository.ensureSlotExists(
                eventId, eventDate, eventTime, maxCapacity != null ? maxCapacity : 0);

        int updated = eventSlotReservationsRepository.tryReserveCapacity(eventId, eventDate, eventTime, qty);
        if (updated == 0) {
            int reserved = getReservedQty(eventId, eventDate, eventTime);
            int max = maxCapacity != null ? maxCapacity : 0;
            int available = Math.max(0, max - reserved);
            String errorMsg = String.format(
                    "Insufficient capacity for %s on %s at %s. Requested: %d, Available: %d",
                    eventName, eventDate, eventTime, qty, available);
            log.warn("Capacity exceeded: {}", errorMsg);
            throw new EventCapacityExceededException(errorMsg);
        }

        log.debug("Reserved {} participant(s) for event {} on {} at {}", qty, eventId, eventDate, eventTime);
    }

    @Transactional
    public void releaseCapacity(Long eventId, LocalDate eventDate, String eventTime, int qty) {
        if (qty <= 0) {
            return;
        }
        int updated = eventSlotReservationsRepository.releaseCapacity(eventId, eventDate, eventTime, qty);
        if (updated == 0) {
            log.warn("No counter row updated when releasing {} for event {} on {} at {}",
                    qty, eventId, eventDate, eventTime);
        } else {
            log.debug("Released {} participant(s) for event {} on {} at {}", qty, eventId, eventDate, eventTime);
        }
    }

    @Transactional(readOnly = true)
    public int getReservedQty(Long eventId, LocalDate eventDate, String eventTime) {
        return eventSlotReservationsRepository.findById(
                        EventSlotReservationId.builder()
                                .eventId(eventId)
                                .eventDate(eventDate)
                                .eventTime(eventTime)
                                .build())
                .map(EventSlotReservations::getReservedQty)
                .orElse(0);
    }

    public boolean countsTowardCapacity(Enums.BookingStatus status) {
        return status == Enums.BookingStatus.ON_HOLD
                || status == Enums.BookingStatus.AWAITING_PAYMENT
                || status == Enums.BookingStatus.PAYMENT_IN_PROGRESS
                || status == Enums.BookingStatus.PAID
                || status == Enums.BookingStatus.CONFIRMED;
    }

    @Transactional
    public void releaseCapacityForBooking(Bookings booking) {
        if (booking == null || booking.getId() == null) {
            return;
        }
        Bookings current = bookingsRepository.findByIdWithLock(booking.getId()).orElse(null);
        if (current == null || !countsTowardCapacity(current.getStatus())) {
            return;
        }
        List<BookingEvents> bookingEvents = bookingEventsRepository.findByBookingId(current.getId());
        boolean released = false;
        for (BookingEvents bookingEvent : orderBookingEventsForSlotUpdate(bookingEvents)) {
            if (bookingEvent.getCancelledAt() == null) {
                releaseCapacityForBookingEvent(bookingEvent);
                released = true;
            }
        }
        if (released) {
            current.setSlotCapacityHeld(false);
            bookingsRepository.save(current);
        }
    }

    @Transactional
    public void reserveCapacityForBooking(Bookings booking) {
        if (booking == null || booking.getId() == null) {
            return;
        }
        List<BookingEvents> bookingEvents = bookingEventsRepository.findByBookingId(booking.getId());
        for (BookingEvents bookingEvent : orderBookingEventsForSlotUpdate(bookingEvents)) {
            if (bookingEvent.getCancelledAt() == null) {
                Events event = bookingEvent.getEvent();
                reserveCapacityForBookingEvent(bookingEvent, event.getMaxCapacity(), event.getName());
            }
        }
    }

    @Transactional
    public void releaseCapacityForBookingEvent(BookingEvents bookingEvent) {
        int qty = sumParticipantQty(bookingEvent.getId());
        if (qty <= 0) {
            return;
        }
        releaseCapacity(
                bookingEvent.getEvent().getId(),
                bookingEvent.getEventDate(),
                bookingEvent.getEventTime(),
                qty);
    }

    @Transactional
    public void reserveCapacityForBookingEvent(BookingEvents bookingEvent, Integer maxCapacity, String eventName) {
        int qty = sumParticipantQty(bookingEvent.getId());
        if (qty <= 0) {
            return;
        }
        reserveCapacity(
                bookingEvent.getEvent().getId(),
                bookingEvent.getEventDate(),
                bookingEvent.getEventTime(),
                qty,
                maxCapacity,
                eventName);
    }

    @Transactional
    public void releaseCapacityForBookingEvents(List<BookingEvents> bookingEvents) {
        for (BookingEvents bookingEvent : orderBookingEventsForSlotUpdate(bookingEvents)) {
            if (countsTowardCapacity(bookingEvent.getBooking().getStatus())) {
                releaseCapacityForBookingEvent(bookingEvent);
            }
        }
    }

    @Transactional
    public void reserveCapacityForBookingEvents(List<BookingEvents> bookingEvents) {
        for (BookingEvents bookingEvent : orderBookingEventsForSlotUpdate(bookingEvents)) {
            if (countsTowardCapacity(bookingEvent.getBooking().getStatus())) {
                Events event = bookingEvent.getEvent();
                reserveCapacityForBookingEvent(bookingEvent, event.getMaxCapacity(), event.getName());
            }
        }
    }

    private List<BookingEvents> orderBookingEventsForSlotUpdate(List<BookingEvents> bookingEvents) {
        if (bookingEvents == null || bookingEvents.isEmpty()) {
            return List.of();
        }

        return bookingEvents.stream()
                .sorted(Comparator
                        .comparing((BookingEvents bookingEvent) -> bookingEvent.getEvent().getId())
                        .thenComparing(BookingEvents::getEventDate)
                        .thenComparing(BookingEvents::getEventTime)
                        .thenComparing(BookingEvents::getId))
                .toList();
    }

    private int sumParticipantQty(Long bookingEventId) {
        Integer total = bookingItemsRepository.sumQuantityByBookingEventId(bookingEventId);
        return total != null ? total : 0;
    }
}
