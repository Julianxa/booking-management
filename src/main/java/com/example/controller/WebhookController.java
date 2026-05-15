package com.example.controller;

import com.example.repository.BookingsRepository;
import com.example.repository.PaymentsRepository;
import com.example.service.WebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Webhooks", description = "Webhook management APIs")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    private final BookingsRepository bookingsRepository;
    private final PaymentsRepository paymentsRepository;
    private final WebhookService webhookService;
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
                case "payment_intent.created" -> webhookService.processPaymentIntentCreated(event);
                case "payment_intent.requires_action" -> webhookService.processPaymentRequiresAction(event);
                case "payment_intent.canceled" -> webhookService.processPaymentIntentCanceled(event);
                case "payment_intent.succeeded", "checkout.session.async_payment_succeeded" ->
                        webhookService.processSuccessfulPayment(event);
                case "payment_intent.payment_failed", "checkout.session.async_payment_failed" ->
                        webhookService.processFailedPayment(event);
                case "checkout.session.expired" -> webhookService.processExpiredPayment(event);
                default -> log.info("Unhandled Stripe event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error processing webhook event: {}", event.getType(), e);
            return ResponseEntity.status(500).body("Internal error");
        }

        return ResponseEntity.ok("Received");
    }
}