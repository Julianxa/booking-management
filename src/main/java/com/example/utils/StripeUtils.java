package com.example.utils;

import com.stripe.Stripe;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripeUtils {
    public PaymentIntent getPaymentIntentFromEvent(Event event) {
        return (PaymentIntent) deserializeEventObject(event, "PaymentIntent");
    }

    public Refund getRefundFromEvent(Event event) {
        return (Refund) deserializeEventObject(event, "Refund");
    }

    public Session getSessionFromEvent(Event event) {
        return (Session) deserializeEventObject(event, "Stripe Session");
    }

    private StripeObject deserializeEventObject(Event event, String objectName) {
        return event.getDataObjectDeserializer()
                .getObject()
                .orElseGet(() -> deserializeUnsafe(event, objectName));
    }

    private StripeObject deserializeUnsafe(Event event, String objectName) {
        try {
            log.warn(
                    "Stripe {} deserialization: API version mismatch (event={}, library={}). Using deserializeUnsafe().",
                    objectName,
                    event.getApiVersion(),
                    Stripe.API_VERSION);
            return event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            throw new IllegalStateException(
                    String.format(
                            "Failed to deserialize %s (eventApiVersion=%s, libraryApiVersion=%s, eventType=%s, eventId=%s)",
                            objectName,
                            event.getApiVersion(),
                            Stripe.API_VERSION,
                            event.getType(),
                            event.getId()),
                    e);
        }
    }
}
