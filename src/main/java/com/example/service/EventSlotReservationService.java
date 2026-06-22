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
import com.example.repository.EventSlotReservationsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSlotReservationService {
    private final EventSlotReservationsRepository eventSlotReservationsRepository;
    private final BookingEventsRepository bookingEventsRepository;
    private final BookingItemsRepository bookingItemsRepository;

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

        eventSlotReservationsRepository.ensureSlotExists(eventId, eventDate, eventTime);

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
        return status == Enums.BookingStatus.PENDING
                || status == Enums.BookingStatus.AWAITING_PAYMENT
                || status == Enums.BookingStatus.PAYMENT_IN_PROGRESS
                || status == Enums.BookingStatus.PAID
                || status == Enums.BookingStatus.SUCCESS;
    }

    @Transactional
    public void releaseCapacityForBooking(Bookings booking) {
        if (!countsTowardCapacity(booking.getStatus())) {
            return;
        }
        List<BookingEvents> bookingEvents = bookingEventsRepository.findByBookingId(booking.getId());
        for (BookingEvents bookingEvent : bookingEvents) {
            if (bookingEvent.getCancelledAt() == null) {
                releaseCapacityForBookingEvent(bookingEvent);
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
        for (BookingEvents bookingEvent : bookingEvents) {
            if (countsTowardCapacity(bookingEvent.getBooking().getStatus())) {
                releaseCapacityForBookingEvent(bookingEvent);
            }
        }
    }

    @Transactional
    public void reserveCapacityForBookingEvents(List<BookingEvents> bookingEvents) {
        for (BookingEvents bookingEvent : bookingEvents) {
            if (countsTowardCapacity(bookingEvent.getBooking().getStatus())) {
                Events event = bookingEvent.getEvent();
                reserveCapacityForBookingEvent(bookingEvent, event.getMaxCapacity(), event.getName());
            }
        }
    }

    private int sumParticipantQty(Long bookingEventId) {
        Integer total = bookingItemsRepository.sumQuantityByBookingEventId(bookingEventId);
        return total != null ? total : 0;
    }
}
