package com.example.service;

import com.example.converter.BookingsConverter;
import com.example.exception.booking.BookingNotFoundException;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.BookingEvents;
import com.example.model.entity.Bookings;
import com.example.model.entity.Users;
import com.example.model.record.GiftCertificateApplicationResult;
import com.example.repository.BookingEventsRepository;
import com.example.repository.BookingsRepository;
import com.example.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.example.constant.Enums.UserRole.AGENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDispatchService {
    private final EmailService emailService;
    private final BookingsRepository bookingsRepository;
    private final BookingEventsRepository bookingEventsRepository;
    private final BookingsConverter bookingsConverter;
    private final GiftCertificateService giftCertificateService;
    private final UsersRepository usersRepository;

    @Async("emailExecutor")
    public void sendPaymentConfirmationEmailsAsync(EmailService.BookingCreatedEvent event) {
        try {
            Bookings booking = reloadBooking(event.booking());
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
            Bookings loaded = reloadBooking(booking);
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs =
                    bookingsConverter.toBookingEventDTOs(loaded, null);
            GiftCertificateApplicationResult giftResult =
                    giftCertificateService.getCertificateRedemptionResult(loaded);
            String promoCode = giftResult != null && giftResult.certificate() != null
                    ? giftResult.certificate().getPromoCode()
                    : null;
            List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets =
                    giftResult != null && giftResult.redeemedTicketTypes() != null
                            ? giftResult.redeemedTicketTypes()
                            : Collections.emptyList();
            dispatchPaymentConfirmationEmails(loaded, bookingEventDTOs, promoCode, redeemedTickets);
        } catch (Exception e) {
            log.error("Failed to dispatch payment confirmation emails for booking {}",
                    booking != null ? booking.getRefNo() : null, e);
        }
    }

    @Async("emailExecutor")
    public void sendBookingConfirmationEmailsAsync(Bookings booking, List<EmailService.BookingEmailPayload> payloads) {
        try {
            Bookings loaded = reloadBooking(booking);
            if (payloads == null || payloads.isEmpty()) {
                log.warn("No booking confirmation email payloads for booking {}", loaded.getRefNo());
                return;
            }

            for (EmailService.BookingEmailPayload payload : payloads) {
                try {
                    BookingEvents bookingEvent = loadBookingEvent(payload.bookingEvent());
                    emailService.sendBookingConfirmationEmail(
                            payload.attendee(),
                            loaded,
                            bookingEvent,
                            payload.tickets(),
                            payload.allAttendees());
                } catch (Exception e) {
                    log.error("Failed to send booking confirmation email to {} for booking {}",
                            payload.attendee() != null ? payload.attendee().getEmail() : null,
                            loaded.getRefNo(),
                            e);
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
            Bookings loaded = reloadBooking(booking);
            if (payloads == null || payloads.isEmpty()) {
                return;
            }
            for (EmailService.BookingEmailPayload payload : payloads) {
                try {
                    BookingEvents bookingEvent = loadBookingEvent(payload.bookingEvent());
                    emailService.sendBookingCancellationEmail(
                            payload.attendee(),
                            loaded,
                            bookingEvent,
                            payload.tickets(),
                            payload.allAttendees());
                } catch (Exception e) {
                    log.error("Failed to send booking cancellation email to {} for booking {}",
                            payload.attendee() != null ? payload.attendee().getEmail() : null,
                            loaded.getRefNo(),
                            e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to dispatch booking cancellation emails for booking {}",
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
            emailService.sendPaymentConfirmationEmail(user, booking, bookingEventDTOs, resolvedPromoCode, resolvedTickets);
            return;
        }

        findPrimaryAttendee(bookingEventDTOs).ifPresentOrElse(attendee -> {
            Users guestRecipient = new Users();
            guestRecipient.setEmail(attendee.getEmail());
            guestRecipient.setFirstName(attendee.getFirstName());
            emailService.sendPaymentConfirmationEmail(
                    guestRecipient, booking, bookingEventDTOs, resolvedPromoCode, resolvedTickets);
        }, () -> log.warn("No primary attendee found for payment confirmation email on booking {}", booking.getRefNo()));
    }

    private Optional<CreateBookingRequestDTO.AttendeeDTO> findPrimaryAttendee(
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {
        return bookingEventDTOs.stream()
                .filter(event -> event.getAttendees() != null && !event.getAttendees().isEmpty())
                .flatMap(event -> event.getAttendees().stream())
                .min(Comparator.comparingInt(CreateBookingRequestDTO.AttendeeDTO::getSequence));
    }

    private Bookings reloadBooking(Bookings booking) {
        if (booking == null || booking.getId() == null) {
            throw new BookingNotFoundException("Booking not found");
        }
        return bookingsRepository.findById(booking.getId())
                .orElseThrow(() -> new BookingNotFoundException(
                        String.format("Booking %s not found", booking.getRefNo())));
    }

    private BookingEvents loadBookingEvent(BookingEvents bookingEvent) {
        if (bookingEvent == null || bookingEvent.getId() == null) {
            return bookingEvent;
        }
        return bookingEventsRepository.findByIdWithBookingAndEvent(bookingEvent.getId())
                .orElse(bookingEvent);
    }
}
