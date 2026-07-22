package com.example.service;

import com.example.converter.BookingsConverter;
import com.example.exception.booking.BookingNotFoundException;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.BookingEvents;
import com.example.model.entity.Bookings;
import com.example.model.entity.Users;
import com.example.model.record.GiftCertificateApplicationResult;
import com.example.repository.BookingEventsRepository;
import com.example.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.example.constant.Enums.UserRole.AGENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDispatchService {
    private final EmailService emailService;
    private final BookingEventsRepository bookingEventsRepository;
    private final BookingsConverter bookingsConverter;
    private final GiftCertificateService giftCertificateService;
    private final UsersRepository usersRepository;

    @Async("emailExecutor")
    public void sendPaymentConfirmationEmailsAsync(EmailService.BookingCreatedEvent event) {
        try {
            Bookings booking = requireBooking(event.booking());
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents = event.bookingEvents();
            if (bookingEvents == null || bookingEvents.isEmpty()) {
                bookingEvents = bookingsConverter.toBookingEventDTOs(booking, null);
            }
            dispatchPaymentConfirmationEmails(
                    booking,
                    bookingEvents,
                    event.promoCode(),
                    event.redeemedTickets());
        } catch (Exception e) {
            log.error("Failed to dispatch payment confirmation emails for booking {}",
                    event.booking() != null ? event.booking().getRefNo() : null, e);
        }
    }

    @Async("emailExecutor")
    public void sendPaymentConfirmationEmailsAsync(Bookings booking) {
        try {
            Bookings resolved = requireBooking(booking);
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs =
                    bookingsConverter.toBookingEventDTOs(resolved, null);
            sendPaymentConfirmationEmailsAsync(resolved, bookingEventDTOs);
        } catch (Exception e) {
            log.error("Failed to dispatch payment confirmation emails for booking {}",
                    booking != null ? booking.getRefNo() : null, e);
        }
    }

    @Async("emailExecutor")
    public void sendPaymentConfirmationEmailsAsync(
            Bookings booking,
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {
        try {
            Bookings resolved = requireBooking(booking);
            if (bookingEventDTOs == null || bookingEventDTOs.isEmpty()) {
                emailService.recordEmailNotSent(
                        resolved, "PAYMENT_CONFIRMATION", null, "No booking events for payment confirmation");
                return;
            }
            GiftCertificateApplicationResult giftResult =
                    giftCertificateService.getCertificateRedemptionResult(resolved);
            String promoCode = giftResult != null && giftResult.certificate() != null
                    ? giftResult.certificate().getPromoCode()
                    : null;
            List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets =
                    giftResult != null && giftResult.redeemedTicketTypes() != null
                            ? giftResult.redeemedTicketTypes()
                            : Collections.emptyList();
            dispatchPaymentConfirmationEmails(resolved, bookingEventDTOs, promoCode, redeemedTickets);
        } catch (Exception e) {
            log.error("Failed to dispatch payment confirmation emails for booking {}",
                    booking != null ? booking.getRefNo() : null, e);
        }
    }

    @Async("emailExecutor")
    public void sendCustomOrBookingConfirmationEmailsAsync(Bookings booking, List<EmailService.BookingEmailPayload> payloads, String emailTemplateRefNo) {
        try {
            Bookings resolved = requireBooking(booking);
            if (payloads == null || payloads.isEmpty()) {
                emailService.recordEmailNotSent(
                        resolved,
                        "BOOKING_CONFIRMATION",
                        null,
                        "No booking confirmation payloads (no attendees on booking events)");
                return;
            }

            for (EmailService.BookingEmailPayload payload : payloads) {
                String recipient = payload.attendee() != null ? payload.attendee().getEmail() : null;
                try {
                    if (recipient == null || recipient.isBlank()) {
                        emailService.recordEmailNotSent(
                                resolved, "BOOKING_CONFIRMATION", recipient, "Attendee email is blank");
                        continue;
                    }
                    BookingEvents bookingEvent = loadBookingEvent(payload.bookingEvent());
                    emailService.sendCustomOrBookingConfirmationEmail(
                            payload.attendee(),
                            resolved,
                            bookingEvent,
                            payload.tickets(),
                            payload.allAttendees(),
                            emailTemplateRefNo);
                } catch (Exception e) {
                    // sendEmail already persists FAILED + failure_reason; keep app log for context
                    log.error("Failed to send booking confirmation email to {} for booking {}",
                            recipient, resolved.getRefNo(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to dispatch booking confirmation emails for booking {}",
                    booking != null ? booking.getRefNo() : null, e);
        }
    }

    @Async("emailExecutor")
    public void sendBookingCancellationEmailsAsync(Bookings booking, List<EmailService.BookingEmailPayload> payloads) {
        try {
            Bookings resolved = requireBooking(booking);
            if (payloads == null || payloads.isEmpty()) {
                return;
            }
            for (EmailService.BookingEmailPayload payload : payloads) {
                try {
                    BookingEvents bookingEvent = loadBookingEvent(payload.bookingEvent());
                    emailService.sendBookingCancellationEmail(
                            payload.attendee(),
                            resolved,
                            bookingEvent,
                            payload.tickets(),
                            payload.allAttendees());
                } catch (Exception e) {
                    log.error("Failed to send booking cancellation email to {} for booking {}",
                            payload.attendee() != null ? payload.attendee().getEmail() : null,
                            resolved.getRefNo(),
                            e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to dispatch booking cancellation emails for booking {}",
                    booking != null ? booking.getRefNo() : null, e);
        }
    }

    @Async("emailExecutor")
    public void sendBookingReminderEmailsAsync(Bookings booking, List<EmailService.BookingEmailPayload> payloads) {
        try {
            Bookings resolved = requireBooking(booking);
            if (payloads == null || payloads.isEmpty()) {
                log.warn("No booking reminder email payloads for booking {}", resolved.getRefNo());
                return;
            }

            for (EmailService.BookingEmailPayload payload : payloads) {
                try {
                    BookingEvents bookingEvent = loadBookingEvent(payload.bookingEvent());
                    emailService.sendBookingReminderEmail(
                            payload.attendee(),
                            resolved,
                            bookingEvent,
                            payload.tickets(),
                            payload.allAttendees());
                } catch (Exception e) {
                    log.error("Failed to send booking reminder email to {} for booking {}",
                            payload.attendee() != null ? payload.attendee().getEmail() : null,
                            resolved.getRefNo(),
                            e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to dispatch booking reminder emails for booking {}",
                    booking != null ? booking.getRefNo() : null, e);
        }
    }

    private void dispatchPaymentConfirmationEmails(
            Bookings booking,
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs,
            String promoCode,
            List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets) {
        String resolvedPromoCode = Objects.toString(promoCode, "");
        List<CreateBookingRequestDTO.TicketTypeDTO> resolvedTickets =
                redeemedTickets != null ? redeemedTickets : Collections.emptyList();

        Users user = booking.getUserId() != null
                ? usersRepository.findById(booking.getUserId()).orElse(null)
                : null;

        if (user != null && user.getRole() == AGENT) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                emailService.recordEmailNotSent(
                        booking, "PAYMENT_CONFIRMATION", user.getEmail(), "Agent user email is blank");
                return;
            }
            try {
                emailService.sendPaymentConfirmationEmail(
                        user, booking, bookingEventDTOs, resolvedPromoCode, resolvedTickets);
            } catch (Exception e) {
                log.error("Failed to send payment confirmation email to agent {} for booking {}",
                        user.getEmail(), booking.getRefNo(), e);
            }
            return;
        }

        List<CreateBookingRequestDTO.AttendeeDTO> primaryAttendees = findPrimaryAttendees(bookingEventDTOs);
        if (primaryAttendees.isEmpty()) {
            emailService.recordEmailNotSent(
                    booking,
                    "PAYMENT_CONFIRMATION",
                    null,
                    "No primary attendee (sequence=1) found on booking events");
            return;
        }

        Map<String, CreateBookingRequestDTO.AttendeeDTO> recipientsByEmail = new LinkedHashMap<>();
        for (CreateBookingRequestDTO.AttendeeDTO attendee : primaryAttendees) {
            if (attendee.getEmail() == null || attendee.getEmail().isBlank()) {
                emailService.recordEmailNotSent(
                        booking, "PAYMENT_CONFIRMATION", attendee.getEmail(), "Primary attendee email is blank");
                continue;
            }
            recipientsByEmail.putIfAbsent(attendee.getEmail().trim().toLowerCase(), attendee);
        }

        if (recipientsByEmail.isEmpty()) {
            emailService.recordEmailNotSent(
                    booking,
                    "PAYMENT_CONFIRMATION",
                    null,
                    "No primary attendee email found for payment confirmation");
            return;
        }

        for (CreateBookingRequestDTO.AttendeeDTO attendee : recipientsByEmail.values()) {
            try {
                Users guestRecipient = new Users();
                guestRecipient.setEmail(attendee.getEmail());
                guestRecipient.setFirstName(attendee.getFirstName());
                emailService.sendPaymentConfirmationEmail(
                        guestRecipient, booking, bookingEventDTOs, resolvedPromoCode, resolvedTickets);
            } catch (Exception e) {
                log.error("Failed to send payment confirmation email to {} for booking {}",
                        attendee.getEmail(), booking.getRefNo(), e);
            }
        }
    }

    private List<CreateBookingRequestDTO.AttendeeDTO> findPrimaryAttendees(
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {
        return bookingEventDTOs.stream()
                .filter(event -> event.getAttendees() != null && !event.getAttendees().isEmpty())
                .flatMap(event -> event.getAttendees().stream())
                .filter(attendee -> attendee.getSequence() == 1)
                .toList();
    }

    private Bookings requireBooking(Bookings booking) {
        if (booking == null || booking.getId() == null) {
            throw new BookingNotFoundException("Booking not found");
        }
        return booking;
    }

    private BookingEvents loadBookingEvent(BookingEvents bookingEvent) {
        if (bookingEvent == null || bookingEvent.getId() == null) {
            return bookingEvent;
        }
        return bookingEventsRepository.findByIdWithBookingAndEvent(bookingEvent.getId())
                .orElse(bookingEvent);
    }
}
