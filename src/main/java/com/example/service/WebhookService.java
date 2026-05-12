package com.example.service;

import com.example.constant.Enums;
import com.example.converter.BookingsConverter;
import com.example.exception.ResourceNotFoundException;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.*;
import com.example.model.record.GiftCertificateApplicationResult;
import com.example.repository.*;
import com.example.utils.DateUtils;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.constant.Enums.UserRole.AGENT;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {
    private final GiftCertificatesRepository giftCertificatesRepository;
    private final UsersRepository usersRepository;
    private final PaymentsRepository paymentsRepository;
    private final BookingsRepository bookingsRepository;
    private final BookingEventsRepository bookingEventsRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final EmailService emailService;
    private final BookingsConverter bookingsConverter;
    private final GiftCertificateService giftCertificateService;
    private final DateUtils dateUtils;

    public record BookingCreatedEvent(
            Users loggedInUser,
            Bookings booking,
            List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents,
            String promoCode,
            List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets,
            List<BookingEmailPayload> emailPayloads
    ) {
    }

    public record BookingEmailPayload(
            CreateBookingRequestDTO.AttendeeDTO attendee,
            BookingEvents bookingEvent,
            List<CreateBookingRequestDTO.TicketTypeDTO> tickets,
            List<CreateBookingRequestDTO.AttendeeDTO> allAttendees
    ) {
    }

    public record BookingReConfirmedEvent(
            Users loggedInUser,
            Bookings booking,
            List<BookingEmailPayload> emailPayloads
    ) {
    }

    @Transactional
    public void confirmPayment(String sessionId, String paymentIntent, String paymentMethod, LocalDateTime paidAt) {
        Payments payment = paymentsRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        updatePaymentRecord(payment, paymentIntent, paymentMethod, paidAt);

        Bookings booking = bookingsRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        updateBookingStatus(booking, Enums.BookingStatus.PAID);

        Users user = usersRepository.findById(booking.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        GiftCertificates giftCertificate = giftCertificatesRepository.findById(booking.getGiftCertificateId()).orElse(null);
        List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents =
                bookingsConverter.toBookingEventDTOs(booking, null);

        GiftCertificateApplicationResult result = handleGiftCertificateRedemption(booking, giftCertificate, user.getId());

        List<WebhookService.BookingEmailPayload> emailPayloads = activateBookingEvents(bookingEvents);

        updateBookingStatus(booking, Enums.BookingStatus.SUCCESS);

        publishBookingConfirmedEvent(user, booking, bookingEvents, result, emailPayloads);
    }

    private GiftCertificateApplicationResult handleGiftCertificateRedemption(
            Bookings booking, GiftCertificates giftCertificate, Long userId) {

        if (giftCertificate == null) {
            return null;
        }

        giftCertificateService.confirmCertificateRedemption(booking, giftCertificate, userId);
        return giftCertificateService.getCertificateRedemptionResult(booking);
    }

    private void updatePaymentRecord(Payments payment, String paymentIntent,
                                     String paymentMethod, LocalDateTime paidAt) {
        payment.setPaymentIntentId(paymentIntent);
        payment.setPaymentChannel(Enums.PaymentChannel.valueOf(paymentMethod.toUpperCase()));
        payment.setPaidAt(paidAt);
        payment.setPaymentStatus(Enums.PaymentStatus.SUCCEEDED);
        paymentsRepository.save(payment);
    }

    private void updateBookingStatus(Bookings booking, Enums.BookingStatus status) {
        booking.setStatus(status);
        bookingsRepository.save(booking);
    }

    private List<WebhookService.BookingEmailPayload> activateBookingEvents(List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents) {
        List<WebhookService.BookingEmailPayload> emailPayloads = new ArrayList<>();

        for (CreateBookingRequestDTO.BookingEventDTO eventDTO : bookingEvents) {
            if (eventDTO.getAttendees() != null) {
                BookingEvents savedBookingEvent = bookingEventsRepository.findByRefNo(eventDTO.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Booking event not found"));

                savedBookingEvent.setStatus(Enums.BookingEventStatus.AVAILABLE);
                bookingEventsRepository.save(savedBookingEvent);

                for (CreateBookingRequestDTO.AttendeeDTO attendee : eventDTO.getAttendees()) {
                    emailPayloads.add(new WebhookService.BookingEmailPayload(
                            attendee, savedBookingEvent, eventDTO.getTickets(), eventDTO.getAttendees()));
                }
            }
        }
        return emailPayloads;
    }

    private void publishBookingConfirmedEvent(Users user, Bookings booking, List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents,
                                              GiftCertificateApplicationResult giftResult,
                                              List<WebhookService.BookingEmailPayload> emailPayloads) {

        String promoCode = giftResult != null ? giftResult.certificate().getPromoCode() : null;
        List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets =
                giftResult != null ? giftResult.redeemedTicketTypes() : null;

        applicationEventPublisher.publishEvent(
                new BookingCreatedEvent(user, booking, bookingEvents, promoCode, redeemedTickets, emailPayloads)
        );
    }

    private void sendBookingOrderSummaryEmailsAsync(Users loggedInUser,
                                                    Bookings booking,
                                                    List<CreateBookingRequestDTO.BookingEventDTO> eventList,
                                                    String promoCode,
                                                    List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets,
                                                    List<WebhookService.BookingEmailPayload> payloads) throws MessagingException {
        if (loggedInUser != null && loggedInUser.getRole() == AGENT) {
            emailService.sendBookingOrderSummaryEmail(loggedInUser, booking, eventList, promoCode, redeemedTickets);
        } else {
            for (WebhookService.BookingEmailPayload payload : payloads) {
                if (payload.attendee().getSequence() == 1) {
                    Users guestAttendee = new Users();
                    guestAttendee.setEmail(payload.attendee().getEmail());
                    guestAttendee.setFirstName(payload.attendee.getFirstName());
                    emailService.sendBookingOrderSummaryEmail(guestAttendee, booking, eventList, promoCode, redeemedTickets);
                }
            }
        }
    }

    private void sendBookingConfirmationEmailsAsync(Bookings booking, List<WebhookService.BookingEmailPayload> payloads) {
        for (WebhookService.BookingEmailPayload payload : payloads) {
            try {
                emailService.sendBookingConfirmationEmail(
                        payload.attendee(),
                        booking,
                        payload.bookingEvent(),
                        payload.tickets(),
                        payload.allAttendees()
                );
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendBookingCancellationEmailsAsync(Bookings booking, List<WebhookService.BookingEmailPayload> payloads) {
        for (WebhookService.BookingEmailPayload payload : payloads) {
            try {
                emailService.sendBookingCancellationEmail(
                        payload.attendee(),
                        booking,
                        payload.bookingEvent(),
                        payload.tickets(),
                        payload.allAttendees()
                );
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    @Transactional
    public void processFailedPayment(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        String sessionId = intent.getPaymentDetails().getOrderReference();

        Payments payment = paymentsRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        payment.setPaymentStatus(Enums.PaymentStatus.FAILED);
        paymentsRepository.save(payment);

        Bookings booking = bookingsRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        booking.setStatus(Enums.BookingStatus.FAILED);
        bookingsRepository.save(booking);
    }

    @Transactional
    public void processPaymentIntentCreated(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        updateBookingStatus(intent.getPaymentDetails().getOrderReference(),
                Enums.BookingStatus.PAYMENT_IN_PROGRESS,
                intent.getId());
    }

    @Transactional
    public void processPaymentIntentCanceled(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        updateBookingStatus(intent.getPaymentDetails().getOrderReference(),
                Enums.BookingStatus.CANCELLED,
                intent.getId());
    }

    @Transactional
    public void processSuccessfulPayment(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        String sessionId = intent.getPaymentDetails().getOrderReference();
        String paymentMethod = intent.getPaymentMethodTypes().get(0);
        String paymentIntent = intent.getId();
        LocalDateTime paidAt = dateUtils.convertToLocalDateTime(intent.getCreated());
        confirmPayment(sessionId, paymentIntent, paymentMethod, paidAt);
    }

    @Transactional
    public void processExpiredPayment(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        String sessionId = intent.getPaymentDetails().getOrderReference();

        Payments payment = paymentsRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        payment.setPaymentStatus(Enums.PaymentStatus.EXPIRED);
        paymentsRepository.save(payment);

        Bookings booking = bookingsRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        booking.setStatus(Enums.BookingStatus.EXPIRED);
        bookingsRepository.save(booking);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCreatedEvent(WebhookService.BookingCreatedEvent event) {
        try {
            sendBookingOrderSummaryEmailsAsync(event.loggedInUser(), event.booking(), event.bookingEvents(), event.promoCode(), event.redeemedTickets(), event.emailPayloads());
            sendBookingConfirmationEmailsAsync(event.booking(), event.emailPayloads());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingReConfirmedEvent(WebhookService.BookingReConfirmedEvent event) {
        try {
            sendBookingConfirmationEmailsAsync(event.booking(), event.emailPayloads());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCancelledEvent(BookingService.BookingCancelledEvent event) {
        try {
            sendBookingCancellationEmailsAsync(event.booking(), event.emailPayloads());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PaymentIntent getPaymentIntentFromEvent(Event event) {
        return (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("Failed to deserialize PaymentIntent"));
    }

    private Session getSessionFromEvent(Event event) {
        return (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("Failed to deserialize Stripe Session"));
    }

    private void updateBookingStatus(String sessionId,
                                     Enums.BookingStatus newStatus,
                                     String paymentIntentId) {

        Payments payment = paymentsRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (paymentIntentId != null) {
            payment.setPaymentIntentId(paymentIntentId);
        }

        Bookings booking = bookingsRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + payment.getBookingId()));

        booking.setStatus(newStatus);

        bookingsRepository.save(booking);
        log.info("Booking {} updated to status: {}", payment.getBookingId(), newStatus);
    }
}
