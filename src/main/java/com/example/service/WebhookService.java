package com.example.service;

import com.example.constant.Enums;
import com.example.converter.BookingsConverter;
import com.example.exception.booking.BookingNotFoundException;
import com.example.exception.booking.BookingEventNotFoundException;
import com.example.exception.payment.PaymentNotFoundException;
import com.example.exception.user.UserNotFoundException;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.*;
import com.example.model.record.GiftCertificateApplicationResult;
import com.example.repository.*;
import com.example.utils.DateUtils;
import com.example.utils.StatusTransitioner;
import com.example.utils.StripeUtils;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.constant.Enums.PaymentPlatform.STRIPE;
import static com.example.constant.Enums.PaymentStatus.INITIATED;


@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {
    private final GiftCertificatesRepository giftCertificatesRepository;
    private final UsersRepository usersRepository;
    private final PaymentsRepository paymentsRepository;
    private final BookingsRepository bookingsRepository;
    private final BookingEventsRepository bookingEventsRepository;
    private final RefundsRepository refundsRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final BookingsConverter bookingsConverter;
    private final GiftCertificateService giftCertificateService;
    private final AuditService auditService;
    private final DateUtils dateUtils;
    private final StatusTransitioner statusTransitioner;
    private final StripeUtils stripeUtils;
    private final EventSlotReservationService eventSlotReservationService;

    // Webhook services
    @Transactional
    public void processPaymentIntentCreated(Event event) {
        PaymentIntent intent = stripeUtils.getPaymentIntentFromEvent(event);
        String bookingRefNo = intent.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));
        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(null, intent.getId(), STRIPE, booking);
        updatePaymentStatus(payment, INITIATED);

        updateBookingStatus(booking, Enums.BookingStatus.PAYMENT_IN_PROGRESS);
        auditService.record("PAYMENT_INTENT_CREATED_WEBHOOK", Payments.class.getName(), payment.getId(), booking.getUserId(), "paymentIntent:" + intent.getId());
    }

    @Transactional
    public void processRefundCreated(Event event) {
        Refund refund = stripeUtils.getRefundFromEvent(event);
        String bookingRefNo = refund.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));

        Optional<Refunds> ongoing = refundsRepository.findByBookingIdAndOngoingStatus(booking.getId());
        if (ongoing.isEmpty()) {
            log.warn("REFUND_CREATED_WEBHOOK: no in-flight refund row for booking {} (stripeRefund={})",
                    booking.getRefNo(), refund.getId());
            return;
        }

        Refunds r = ongoing.get();
        updateRefundStatus(r, Enums.RefundStatus.PROCESSING);
        auditService.record("REFUND_CREATED_WEBHOOK", Refunds.class.getName(), r.getId(), booking.getUserId(), "refund:" + refund.getId());
    }

    @Transactional
    public void processRefundUpdated(Event event) {
        Refund refund = stripeUtils.getRefundFromEvent(event);
        String bookingRefNo = refund.getMetadata().get("bookingRefNo");
        String paymentIntentId = refund.getMetadata().get("paymentIntentId");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));

        if (booking.getStatus() == Enums.BookingStatus.REFUNDED) {
            log.info("Ignoring duplicate refund.updated for booking {}", booking.getRefNo());
            return;
        }

        Payments payment = paymentsRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException(String.format("Payment %s not found", paymentIntentId)));

        Optional<Refunds> ongoing = refundsRepository.findByBookingIdAndOngoingStatus(booking.getId());
        if (ongoing.isEmpty()) {
            log.warn("REFUND_UPDATED_WEBHOOK: no in-flight refund row for booking {} (stripeRefund={})",
                    booking.getRefNo(), refund.getId());
            return;
        }

        Refunds r = ongoing.get();
        String stripeStatus = refund.getStatus();

        if ("failed".equals(stripeStatus)) {
            updateRefundStatus(r, Enums.RefundStatus.FAILED);
            auditService.record("REFUND_UPDATED_WEBHOOK", Refunds.class.getName(), r.getId(), booking.getUserId(),
                    "refund failed:" + r.getId() + ", paymentIntent:" + paymentIntentId);
            return;
        }

        if (!"succeeded".equals(stripeStatus)) {
            if ("pending".equals(stripeStatus)) {
                updateRefundStatus(r, Enums.RefundStatus.PROCESSING);
            }
            log.info("Ignoring refund.updated with status {} for booking {}", stripeStatus, booking.getRefNo());
            return;
        }

        updateRefundStatus(r, Enums.RefundStatus.SUCCESS);
        eventSlotReservationService.releaseCapacityForBooking(booking);
        updateBookingStatus(booking, Enums.BookingStatus.REFUNDED);
        updatePaymentStatus(payment, Enums.PaymentStatus.REFUNDED);
        auditService.record("REFUND_UPDATED_WEBHOOK", Refunds.class.getName(), r.getId(), booking.getUserId(), "refund:" + r.getId() + ", paymentIntent:" + paymentIntentId);
    }

    @Transactional
    public void processRefundFailed(Event event) {
        Refund refund = stripeUtils.getRefundFromEvent(event);
        String bookingRefNo = refund.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));

        Optional<Refunds> ongoing = refundsRepository.findByBookingIdAndOngoingStatus(booking.getId());
        if (ongoing.isEmpty()) {
            log.warn("REFUND_FAILED_WEBHOOK: no in-flight refund row for booking {} (stripeRefund={})",
                    booking.getRefNo(), refund.getId());
            return;
        }

        Refunds r = ongoing.get();
        updateRefundStatus(r, Enums.RefundStatus.FAILED);
        auditService.record("REFUND_FAILED_WEBHOOK", Refunds.class.getName(), r.getId(), booking.getUserId(), "refund:" + r.getId());
    }

    @Transactional
    public void processPaymentRequiresAction(Event event) {
        PaymentIntent intent = stripeUtils.getPaymentIntentFromEvent(event);
        String bookingRefNo = intent.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));
        String sessionId = intent.getPaymentDetails().getOrderReference();
        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(sessionId, intent.getId(), STRIPE, booking);

        updatePaymentStatus(payment, Enums.PaymentStatus.REQUIRES_ACTION);
        updateBookingStatus(booking, Enums.BookingStatus.PAYMENT_IN_PROGRESS);

        auditService.record("PAYMENT_REQUIRES_ACTION_WEBHOOK", Payments.class.getName(), payment.getId(), booking.getUserId(), "payment:" + payment.getId());
    }

    @Transactional
    public void processFailedPayment(Event event) {
        PaymentIntent intent = stripeUtils.getPaymentIntentFromEvent(event);
        String sessionId = intent.getPaymentDetails() != null
                ? intent.getPaymentDetails().getOrderReference()
                : null;

        String bookingRefNo = intent.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));

        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(sessionId, intent.getId(), STRIPE, booking);
        if (shouldIgnorePaymentFailureWebhook(booking, payment)) {
            log.info("Ignoring payment_intent.payment_failed for booking {} (status={}, payment={})",
                    booking.getRefNo(), booking.getStatus(), payment.getPaymentStatus());
            return;
        }

        updatePaymentStatus(payment, Enums.PaymentStatus.FAILED);
        giftCertificateService.cancelCertificateRedemption(booking);
        eventSlotReservationService.releaseCapacityForBooking(booking);
        updateBookingStatus(booking, Enums.BookingStatus.FAILED);
        auditService.record("PAYMENT_FAILED_WEBHOOK", Payments.class.getName(), booking.getId(), booking.getUserId(), "paymentIntent:" + intent.getId());
    }

    @Transactional
    public void processCheckoutSessionAsyncPaymentFailed(Event event) {
        Session session = stripeUtils.getSessionFromEvent(event);
        String bookingRefNo = session.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException("Booking " + bookingRefNo + " not found"));
        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(
                session.getId(), session.getPaymentIntent(), STRIPE, booking);
        if (shouldIgnorePaymentFailureWebhook(booking, payment)) {
            log.info("Ignoring checkout.session.async_payment_failed for booking {} (status={}, payment={})",
                    booking.getRefNo(), booking.getStatus(), payment.getPaymentStatus());
            return;
        }
        updatePaymentStatus(payment, Enums.PaymentStatus.FAILED);
        giftCertificateService.cancelCertificateRedemption(booking);
        eventSlotReservationService.releaseCapacityForBooking(booking);
        updateBookingStatus(booking, Enums.BookingStatus.FAILED);
    }

    @Transactional
    public void processPaymentIntentCanceled(Event event) {
        PaymentIntent intent = stripeUtils.getPaymentIntentFromEvent(event);
        String sessionId = intent.getPaymentDetails() != null
                ? intent.getPaymentDetails().getOrderReference()
                : null;
        String bookingRefNo = intent.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));
        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(sessionId, intent.getId(), STRIPE, booking);
        if (shouldIgnorePaymentFailureWebhook(booking, payment)) {
            log.info("Ignoring payment_intent.canceled for booking {} (status={}, payment={})",
                    booking.getRefNo(), booking.getStatus(), payment.getPaymentStatus());
            return;
        }

        updatePaymentStatus(payment, Enums.PaymentStatus.CANCELLED);
        giftCertificateService.cancelCertificateRedemption(booking);
        eventSlotReservationService.releaseCapacityForBooking(booking);
        updateBookingStatus(booking, Enums.BookingStatus.FAILED);

        auditService.record("PAYMENT_CANCELLED_WEBHOOK", Payments.class.getName(), booking.getId(), booking.getUserId(), "paymentIntent:" + intent.getId());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processSuccessfulPayment(Event event) {
        Users user = null;
        PaymentIntent intent = stripeUtils.getPaymentIntentFromEvent(event);
        String sessionId = intent.getPaymentDetails().getOrderReference();
        String paymentMethod = intent.getPaymentMethodTypes().get(0);
        String paymentIntent = intent.getId();
        ZonedDateTime paidAt = dateUtils.convertToZonedDateTime(intent.getCreated());

        String bookingRefNo = intent.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));
        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(sessionId, intent.getId(), STRIPE, booking);

        if(booking.getUserId() != null)
            user = usersRepository.findById(booking.getUserId())
                    .orElseThrow(() -> new UserNotFoundException(String.format("User %s not found", booking.getUserId())));
        confirmOnlinePayment(user, booking, payment, paymentIntent, paymentMethod, paidAt);
        auditService.record("PAYMENT_SUCCEEDED_WEBHOOK", Payments.class.getName(), booking.getId(), booking.getUserId(), "paymentIntent:" + paymentIntent);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processCheckoutSessionAsyncPaymentSucceeded(Event event) {
        Session session = stripeUtils.getSessionFromEvent(event);
        String bookingRefNo = session.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException("Booking " + bookingRefNo + " not found"));

        if (booking.getStatus() == Enums.BookingStatus.SUCCESS
                || booking.getStatus() == Enums.BookingStatus.PAID) {
            return;
        }
        String paymentIntentId = session.getPaymentIntent();
        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(
                session.getId(), paymentIntentId, STRIPE, booking);
        Users user = null;
        if (booking.getUserId() != null) {
            user = usersRepository.findById(booking.getUserId())
                    .orElseThrow(() -> new UserNotFoundException("User " + booking.getUserId() + " not found"));
        }
        String paymentMethod = resolvePaymentMethod(session);
        ZonedDateTime paidAt = dateUtils.convertToZonedDateTime(session.getCreated());
        confirmOnlinePayment(user, booking, payment, paymentIntentId, paymentMethod, paidAt);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processExpiredPayment(Event event) {
        Session session = stripeUtils.getSessionFromEvent(event);
        String sessionId = session.getId();

        String bookingRefNo = session.getMetadata().get("bookingRefNo");
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));
        Payments payment = paymentService.findOrCreatePaymentByPaymentIntentId(sessionId, null, STRIPE, booking);

        if (shouldIgnorePaymentFailureWebhook(booking, payment)) {
            log.info("Ignoring checkout.session.expired for booking {} (status={}, payment={})",
                    booking.getRefNo(), booking.getStatus(), payment.getPaymentStatus());
            return;
        }

        updatePaymentStatus(payment, Enums.PaymentStatus.EXPIRED);
        giftCertificateService.cancelCertificateRedemption(booking);
        eventSlotReservationService.releaseCapacityForBooking(booking);
        updateBookingStatus(booking, Enums.BookingStatus.EXPIRED);
        auditService.record("PAYMENT_EXPIRED_WEBHOOK", Payments.class.getName(), booking.getId(), booking.getUserId(), "sessionId:" + sessionId);
    }

    // Utility functions
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void confirmOnlinePayment(Users user, Bookings booking, Payments payment, String paymentIntent, String paymentMethod, ZonedDateTime paidAt) {
        if (booking.getStatus() == Enums.BookingStatus.SUCCESS
                || booking.getStatus() == Enums.BookingStatus.PAID) {
            return;
        }

        updateSuccessPaymentRecord(payment, paymentIntent, paymentMethod, Enums.PaymentStatus.SUCCEEDED, paidAt);

        updateBookingStatus(booking, Enums.BookingStatus.PAID);

        GiftCertificates giftCertificate = Optional.ofNullable(booking.getGiftCertificateId())
                .map(giftCertificatesRepository::findById)
                .map(opt -> opt.orElse(null))
                .orElse(null);
        List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents = bookingsConverter.toBookingEventDTOs(booking, null);

        GiftCertificateApplicationResult result = giftCertificateService.confirmCertificateRedemption(booking, giftCertificate, user != null ? user.getId() : null);

        List<EmailService.BookingEmailPayload> emailPayloads = activateBookingEvents(bookingEvents);

        updateBookingStatus(booking, Enums.BookingStatus.SUCCESS);

        publishBookingConfirmedEvent(user, booking, bookingEvents, result, emailPayloads);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void confirmOfflinePayment(Users user, Bookings booking) {
        updateBookingStatus(booking, Enums.BookingStatus.PAID);

        GiftCertificates giftCertificate = Optional.ofNullable(booking.getGiftCertificateId())
                .map(giftCertificatesRepository::findById)
                .map(opt -> opt.orElse(null))
                .orElse(null);
        List<CreateBookingRequestDTO.BookingEventDTO> bookingEvents = bookingsConverter.toBookingEventDTOs(booking, null);

        GiftCertificateApplicationResult result = giftCertificateService.confirmCertificateRedemption(booking, giftCertificate, user != null ? user.getId() : null);

        List<EmailService.BookingEmailPayload> emailPayloads = activateBookingEvents(bookingEvents);

        updateBookingStatus(booking, Enums.BookingStatus.SUCCESS);

        publishBookingConfirmedEvent(user, booking, bookingEvents, result, emailPayloads);
    }

    @Transactional
    public List<EmailService.BookingEmailPayload> activateBookingEvents(List<CreateBookingRequestDTO.BookingEventDTO> bookingEventDTOs) {
        List<EmailService.BookingEmailPayload> emailPayloads = new ArrayList<>();

        for (CreateBookingRequestDTO.BookingEventDTO bookingEventDTO : bookingEventDTOs) {
            if (bookingEventDTO.getAttendees() != null) {
                BookingEvents savedBookingEvent = bookingEventsRepository.findByRefNo(bookingEventDTO.getId())
                        .orElseThrow(() -> new BookingEventNotFoundException(String.format("Booking event %s not found", bookingEventDTO.getId())));

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

    // Listener functions
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCreatedEvent(EmailService.BookingCreatedEvent event) {
        emailService.sendPaymentConfirmationEmailsAsync(event.booking());
        emailService.sendBookingConfirmationEmailsAsync(event.booking(), event.emailPayloads());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingRestoreEvent(EmailService.BookingRestoreEvent event) {
        emailService.sendBookingConfirmationEmailsAsync(event.booking(), event.emailPayloads());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCancelledEvent(BookingService.BookingCancelledEvent event) {
        emailService.sendBookingCancellationEmailsAsync(event.booking(), event.emailPayloads());
    }

    void updateBookingStatus(Bookings booking, Enums.BookingStatus status) {
        if(statusTransitioner.shouldUpdateBookingStatus(booking.getStatus(), status)) {
            booking.setStatus(status);
            bookingsRepository.save(booking);
        }
    }

    public void updateSuccessPaymentRecord(Payments payment, String paymentIntent,
                                    String paymentMethod, Enums.PaymentStatus status, ZonedDateTime paidAt) {
        updatePaymentStatus(payment, status);
        payment.setPaymentIntentId(paymentIntent);
        payment.setPaymentChannel(paymentMethod == null ? null : Enums.PaymentChannel.valueOf(paymentMethod.toUpperCase()));
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

    void updateRefundStatus(Refunds refund, Enums.RefundStatus status) {
        if (refund == null || status == null) {
            return;
        }
        if(statusTransitioner.shouldUpdateRefundStatus(refund.getStatus(), status)) {
            refund.setStatus(status);
            refundsRepository.save(refund);
        }
    }

    private boolean isTerminalBookingState(Enums.BookingStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case SUCCESS, PAID, REFUNDED, CANCELLED, FAILED, EXPIRED -> true;
            default -> false;
        };
    }

    private boolean isPaymentAlreadySucceeded(Payments payment) {
        return payment != null && payment.getPaymentStatus() == Enums.PaymentStatus.SUCCEEDED;
    }

    private boolean shouldIgnorePaymentFailureWebhook(Bookings booking, Payments payment) {
        return isTerminalBookingState(booking.getStatus()) || isPaymentAlreadySucceeded(payment);
    }

    private String resolvePaymentMethod(Session session) {
        if (session.getPaymentMethodTypes() != null && !session.getPaymentMethodTypes().isEmpty()) {
            return session.getPaymentMethodTypes().get(0);
        }
        return "card";
    }
}
