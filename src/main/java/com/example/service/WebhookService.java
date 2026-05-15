package com.example.service;

import com.example.constant.Enums;
import com.example.converter.BookingsConverter;
import com.example.exception.ResourceNotFoundException;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.*;
import com.example.model.record.GiftCertificateApplicationResult;
import com.example.repository.*;
import com.example.utils.DateUtils;
import com.example.utils.StatusTransitioner;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
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
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final BookingsConverter bookingsConverter;
    private final GiftCertificateService giftCertificateService;
    private final DateUtils dateUtils;
    private final StatusTransitioner statusTransitioner;

    @Transactional
    public void confirmPayment(Payments payment, String paymentIntent, String paymentMethod, LocalDateTime paidAt) {
        updatePaymentRecord(payment, paymentIntent, paymentMethod, Enums.PaymentStatus.SUCCEEDED, paidAt);

        Bookings booking = bookingsRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        updateBookingStatus(booking, Enums.BookingStatus.PAID);

        Users user = null;
        if(booking.getUserId() != null) {
            user = usersRepository.findById(booking.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        GiftCertificates giftCertificate = giftCertificatesRepository.findById(booking.getGiftCertificateId()).orElse(null);
        List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents =
                bookingsConverter.toBookingEventDTOs(booking, null);

        GiftCertificateApplicationResult result = giftCertificateService.handleGiftCertificateRedemption(booking, giftCertificate, user != null ? user.getId() : null);

        List<EmailService.BookingEmailPayload> emailPayloads = activateBookingEvents(bookingEvents);

        updateBookingStatus(booking, Enums.BookingStatus.SUCCESS);

        publishBookingConfirmedEvent(user, booking, bookingEvents, result, emailPayloads);
    }

    public List<EmailService.BookingEmailPayload> activateBookingEvents(List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {
        List<EmailService.BookingEmailPayload> emailPayloads = new ArrayList<>();

        for (CreateBookingRequestDTO.BookingEventDTO bookingEventDTO : bookingEventDTOs) {
            if (bookingEventDTO.getAttendees() != null) {
                BookingEvents savedBookingEvent = bookingEventsRepository.findByRefNo(bookingEventDTO.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Booking event not found"));

                savedBookingEvent.setStatus(Enums.BookingEventStatus.AVAILABLE);
                bookingEventsRepository.save(savedBookingEvent);

                bookingEventDTO.setStatus(Enums.BookingEventStatus.AVAILABLE);

                for (CreateBookingRequestDTO.AttendeeDTO attendee : bookingEventDTO.getAttendees()) {
                    emailPayloads.add(new EmailService.BookingEmailPayload(
                            attendee, savedBookingEvent, bookingEventDTO.getTickets(), bookingEventDTO.getAttendees()));
                }
            }
        }
        return emailPayloads;
    }

    public void publishBookingConfirmedEvent(Users user, Bookings booking, List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents,
                                      GiftCertificateApplicationResult giftResult,
                                      List<EmailService.BookingEmailPayload> emailPayloads) {

        String promoCode = giftResult != null ? giftResult.certificate().getPromoCode() : null;
        List<CreateBookingRequestDTO.TicketTypeDTO> redeemedTickets =
                giftResult != null ? giftResult.redeemedTicketTypes() : null;

        applicationEventPublisher.publishEvent(
                new EmailService.BookingCreatedEvent(user, booking, bookingEvents, promoCode, redeemedTickets, emailPayloads)
        );
    }

    private PaymentIntent getPaymentIntentFromEvent(Event event) {
        return (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("Failed to deserialize PaymentIntent"));
    }

    // Customer opens the cashier page and starts checkout
    @Transactional
    public void processPaymentIntentCreated(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        String bookingRefNo = intent.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        String sessionId = intent.getPaymentDetails().getOrderReference();
        paymentService.findOrCreatePaymentByPaymentIntentId(sessionId, intent.getId(), booking);

        updateBookingStatus(booking, Enums.BookingStatus.PAYMENT_IN_PROGRESS);
    }

    @Transactional
    public void processPaymentRequiresAction(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        String bookingRefNo = intent.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        String sessionId = intent.getPaymentDetails().getOrderReference();
        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(sessionId, intent.getId(), booking);

        updatePaymentStatus(payment, Enums.PaymentStatus.REQUIRES_ACTION);
        updateBookingStatus(booking, Enums.BookingStatus.PAYMENT_IN_PROGRESS);
    }

    @Transactional
    public void processFailedPayment(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        String sessionId = intent.getPaymentDetails().getOrderReference();

        String bookingRefNo = intent.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(sessionId, intent.getId(), booking);
        updatePaymentStatus(payment, Enums.PaymentStatus.FAILED);

        booking.setStatus(Enums.BookingStatus.FAILED);
        bookingsRepository.save(booking);
    }

    @Transactional
    public void processPaymentIntentCanceled(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        String sessionId = intent.getPaymentDetails().getOrderReference();
        String bookingRefNo = intent.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(sessionId, intent.getId(), booking);
        updatePaymentStatus(payment, Enums.PaymentStatus.CANCELLED);
    }

    @Transactional
    public void processSuccessfulPayment(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        String sessionId = intent.getPaymentDetails().getOrderReference();
        String paymentMethod = intent.getPaymentMethodTypes().get(0);
        String paymentIntent = intent.getId();
        LocalDateTime paidAt = dateUtils.convertToLocalDateTime(intent.getCreated());

        String bookingRefNo = intent.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(sessionId, intent.getId(), booking);

        confirmPayment(payment, paymentIntent, paymentMethod, paidAt);
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
    public void handleBookingCreatedEvent(EmailService.BookingCreatedEvent event) {
        try {
            emailService.sendBookingOrderSummaryEmailsAsync(event.loggedInUser(), event.booking(), event.bookingEvents(), event.promoCode(), event.redeemedTickets(), event.emailPayloads());
            emailService.sendBookingConfirmationEmailsAsync(event.booking(), event.emailPayloads());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingReConfirmedEvent(EmailService.BookingReConfirmedEvent event) {
        try {
            emailService.sendBookingConfirmationEmailsAsync(event.booking(), event.emailPayloads());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCancelledEvent(BookingService.BookingCancelledEvent event) {
        try {
            emailService.sendBookingCancellationEmailsAsync(event.booking(), event.emailPayloads());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void updateBookingStatus(Bookings booking, Enums.BookingStatus status) {
        if(statusTransitioner.shouldUpdateBookingStatus(booking.getStatus(), status)) {
            booking.setStatus(status);
            bookingsRepository.save(booking);
        }
    }

    public void updatePaymentRecord(Payments payment, String paymentIntent,
                                    String paymentMethod, Enums.PaymentStatus status, LocalDateTime paidAt) {
        updatePaymentStatus(payment, status);
        payment.setPaymentIntentId(paymentIntent);
        payment.setPaymentChannel(Enums.PaymentChannel.valueOf(paymentMethod.toUpperCase()));
        payment.setPaidAt(paidAt);
        paymentsRepository.save(payment);
    }

    void updatePaymentStatus(Payments payment, Enums.PaymentStatus status) {
        if (status != null) {
            if(statusTransitioner.shouldUpdatePaymentStatus(payment.getPaymentStatus(), status)) {
                payment.setPaymentStatus(status);
                paymentsRepository.save(payment);
            }
        }
    }
}
