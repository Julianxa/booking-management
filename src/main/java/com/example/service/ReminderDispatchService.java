package com.example.service;

import com.example.converter.BookingItemsConverter;
import com.example.exception.booking.BookingEventNotFoundException;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.BookingEvents;
import com.example.model.entity.BookingItems;
import com.example.model.entity.Bookings;
import com.example.repository.BookingAttendeesRepository;
import com.example.repository.BookingEventsRepository;
import com.example.repository.BookingItemsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderDispatchService {
    private final ReminderService reminderService;
    private final BookingEventsRepository bookingEventsRepository;
    private final BookingAttendeesRepository attendeesRepository;
    private final BookingItemsRepository bookingItemsRepository;
    private final BookingItemsConverter bookingItemsConverter;
    private final EmailService emailService;

    @Async("reminderExecutor")
    public void dispatchReminderForEvent(Long bookingEventId) {
        try {
            boolean allSentSuccessfully = sendRemindersForEvent(bookingEventId);
            if (allSentSuccessfully) {
                reminderService.markReminderSent(bookingEventId);
                log.info("Successfully sent reminder for booking event ID: {}", bookingEventId);
            } else {
                reminderService.releaseReminderClaim(bookingEventId);
                log.warn("Released reminder claim for booking event ID {} — will retry on next run", bookingEventId);
            }
        } catch (Exception e) {
            reminderService.releaseReminderClaim(bookingEventId);
            log.error("Failed to process reminder for booking event ID: {}", bookingEventId, e);
        }
    }

    private boolean sendRemindersForEvent(Long bookingEventId) {
        BookingEvents bookingEvent = bookingEventsRepository.findByIdWithBookingAndEvent(bookingEventId)
                .orElseThrow(() -> new BookingEventNotFoundException(
                        String.format("Booking event %s not found", bookingEventId)));

        List<CreateBookingRequestDTO.AttendeeDTO> attendees =
                attendeesRepository.findAttendeesByBookingEventId(bookingEventId);

        if (attendees.isEmpty()) {
            log.warn("No attendees found for booking event ID: {} — marking complete", bookingEventId);
            return true;
        }

        Bookings booking = bookingEvent.getBooking();

        List<BookingItems> bookingItems = bookingItemsRepository.findByBookingEventId(bookingEventId);
        List<CreateBookingRequestDTO.TicketTypeDTO> ticketDTOs =
                bookingItemsConverter.toTicketTypeDTOs(bookingItems);

        boolean allSentSuccessfully = true;

        for (CreateBookingRequestDTO.AttendeeDTO attendee : attendees) {
            try {
                emailService.sendBookingReminderEmail(attendee, booking, bookingEvent, ticketDTOs, attendees);
            } catch (Exception e) {
                allSentSuccessfully = false;
                log.error("Failed to send reminder to {}", attendee.getEmail(), e);
            }
        }

        return allSentSuccessfully;
    }
}
