package com.example.controller;

import com.example.constant.Enums;
import com.example.exception.ResourceNotFoundException;
import com.example.model.entity.*;
import com.example.repository.BookingsRepository;
import com.example.repository.PaymentsRepository;
import com.example.service.WebhookService;
import com.example.utils.DateUtils;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Tag(name = "Webhooks", description = "Webhook management APIs")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    private final BookingsRepository bookingsRepository;
    private final PaymentsRepository paymentsRepository;
    private final WebhookService webhookService;
    private final DateUtils dateUtils;
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Stripe webhook received: Type={}, EventId={}",
                    event.getType(), event.getId());
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe signature", e);
            return ResponseEntity.status(400).body("Invalid signature");
        }

        try {
            switch (event.getType()) {
                case "payment_intent.created" -> processPaymentIntentCreated(event);
                case "payment_intent.canceled" -> processPaymentIntentCanceled(event);
                case "payment_intent.succeeded", "checkout.session.async_payment_succeeded" ->
                        processSuccessfulPayment(event);
                case "payment_intent.payment_failed", "checkout.session.async_payment_failed" -> processFailedPayment(event);
                case "checkout.session.expired" -> processExpiredPayment(event);
                default -> log.info("Unhandled Stripe event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error processing webhook event: {}", event.getType(), e);
            return ResponseEntity.status(500).body("Internal error");
        }

        return ResponseEntity.ok("Received");
    }

    @Transactional
    private void processPaymentIntentCreated(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        updateBookingStatus(intent.getPaymentDetails().getOrderReference(),
                Enums.BookingStatus.PAYMENT_IN_PROGRESS,
                intent.getId());
    }

    @Transactional
    private void processPaymentIntentCanceled(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        updateBookingStatus(intent.getPaymentDetails().getOrderReference(),
                Enums.BookingStatus.CANCELLED,
                intent.getId());
    }

    @Transactional
    private void processSuccessfulPayment(Event event) {
        PaymentIntent intent = getPaymentIntentFromEvent(event);
        String sessionId = intent.getPaymentDetails().getOrderReference();
        String paymentMethod = intent.getPaymentMethodTypes().get(0);
        String paymentIntent = intent.getId();
        LocalDateTime paidAt = dateUtils.convertToLocalDateTime(intent.getCreated());
        webhookService.confirmPayment(sessionId, paymentIntent, paymentMethod, paidAt);
    }

    @Transactional
    private void processFailedPayment(Event event) {
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
    private void processExpiredPayment(Event event) {
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

    private Session getSessionFromEvent(Event event) {
        return (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("Failed to deserialize Stripe Session"));
    }

    private PaymentIntent getPaymentIntentFromEvent(Event event) {
        return (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("Failed to deserialize PaymentIntent"));
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