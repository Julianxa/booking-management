package com.example.scheduler;


import com.example.converter.BookingItemsConverter;
import com.example.exception.booking.BookingNotFoundException;
import com.example.exception.email.MissingIntervalException;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.*;
import com.example.repository.*;
import com.example.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
public class ReminderScheduler {
    @Autowired
    EmailTemplatesRepository templatesRepository;

    @Autowired
    BookingEventsRepository bookingEventsRepository;

    @Autowired
    BookingAttendeesRepository attendeesRepository;

    @Autowired
    BookingsRepository bookingsRepository;

    @Autowired
    BookingItemsRepository bookingItemsRepository;

    @Autowired
    BookingItemsConverter bookingItemsConverter;

    @Autowired
    EmailService emailService;

    // Run every day at 9:00 AM Hong Kong Time
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Hong_Kong")
    @Transactional
    public void sendOneDayBeforeReminders() {
        sendRemindersForTomorrow();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void sendRemindersOnStartup() {
        log.info("Application started - checking for pending reminders...");
        sendRemindersForTomorrow();
    }

    private void sendRemindersForTomorrow() {
        Integer reminderInterval = templatesRepository
                .findReminderDayInterval()
                .orElseThrow(() -> new MissingIntervalException("Missing reminder_day_interval in template"));

        LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Hong_Kong"))
                .plusDays(reminderInterval);

        List<BookingEvents> bookingEvents = bookingEventsRepository
                .findUpcomingEventsForReminder(targetDate);

        for (BookingEvents bookingEvent : bookingEvents) {
            if (bookingEvent.getReminderSentAt() != null) {
                continue;
            }

            try {
                processReminderForEvent(bookingEvent);
            } catch (Exception e) {
                log.error("Failed to process reminder for booking event ID: {}", bookingEvent.getId(), e);
            }
        }
    }

    private void processReminderForEvent(BookingEvents bookingEvent) {
        List<CreateBookingRequestDTO.AttendeeDTO> attendees =
                attendeesRepository.findAttendeesByBookingEventId(bookingEvent.getId());

        if (attendees.isEmpty()) {
            log.warn("No attendees found for booking event ID: {}", bookingEvent.getId());
            return;
        }

        Bookings booking = bookingsRepository.findById(bookingEvent.getBooking().getId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        List<BookingItems> bookingItems = bookingItemsRepository.findByBookingEventId(bookingEvent.getId());
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

        if (allSentSuccessfully) {
            bookingEvent.setReminderSentAt(ZonedDateTime.now(ZoneId.systemDefault()));
            bookingEventsRepository.save(bookingEvent);
            log.info("Successfully sent reminder for booking event ID: {}", bookingEvent.getId());
        }
    }
}