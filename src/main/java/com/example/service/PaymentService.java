package com.example.service;


import com.example.constant.Enums;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.entity.Bookings;
import com.example.model.entity.Payments;
import com.example.repository.BookingsRepository;
import com.example.repository.PaymentsRepository;
import com.example.utils.ReferenceNoGenerator;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.SQLException;

import static com.example.constant.Enums.PaymentPlatform.STRIPE;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final StripeClient stripeClient;
    private final PaymentsRepository paymentsRepository;
    private final BookingsRepository bookingsRepository;
    private final ReferenceNoGenerator referenceNoGenerator;

    @Transactional
    public String createCheckoutSession(String userSub, CreateBookingRequestDTO request, Bookings booking) throws StripeException, SQLException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(request.getSuccessUrl())
                .setCancelUrl(request.getCancelUrl())
//                .setCustomerEmail(getUserEmail(userSub))
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(booking.getCurrency())
                                .setUnitAmount(booking.getFinalPaidAmount().multiply(BigDecimal.valueOf(100)).longValueExact())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Booking Payment")
                                        .setDescription("Booking Ref: " + booking.getRefNo())
                                        .build())
                                .build())
                        .build())
                .putMetadata("bookingRefNo", booking.getRefNo())
                .putMetadata("userSub", userSub)
                .build();

        Session session = stripeClient.v1().checkout().sessions().create(params);

        Payments payments = Payments.builder()
                .refNo(referenceNoGenerator.generatePaymentReference())
                .bookingId(booking.getId())
                .amount(booking.getFinalPaidAmount())
                .currency(booking.getCurrency())
                .paymentPlatform(STRIPE)
                .sessionId(session.getId())
                .paymentIntentId(session.getPaymentIntent())
                .paymentStatus(Enums.PaymentStatus.PENDING)
                .build();
        paymentsRepository.save(payments);

        booking.setStatus(Enums.BookingStatus.PAYMENT_PENDING);
        bookingsRepository.save(booking);

        return session.getUrl();
    }
}
