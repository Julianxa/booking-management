package com.example.service;


import com.example.constant.Enums;
import com.example.exception.booking.BookingNotFoundException;
import com.example.exception.payment.*;
import com.example.model.dto.CreateBookingRequestDTO;
import com.example.model.dto.GetPaymentDetailsResponseDTO;
import com.example.model.dto.RefundRequestDTO;
import com.example.model.dto.RefundResponseDTO;
import com.example.model.entity.Bookings;
import com.example.model.entity.Payments;
import com.example.model.entity.Refunds;
import com.example.repository.BookingsRepository;
import com.example.repository.PaymentsRepository;
import com.example.repository.RefundsRepository;
import com.example.utils.ReferenceNoGenerator;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static com.example.constant.Enums.PaymentPlatform.STRIPE;
import static com.stripe.param.checkout.SessionCreateParams.PaymentMethodOptions.WechatPay.Client.WEB;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final StripeClient stripeClient;
    private final PaymentsRepository paymentsRepository;
    private final BookingsRepository bookingsRepository;
    private final RefundsRepository refundsRepository;
    private final ReferenceNoGenerator referenceNoGenerator;

    @Transactional
    public String createCheckoutSession(String userSub, CreateBookingRequestDTO request, Bookings booking) {
        SessionCreateParams.PaymentMethodOptions paymentMethodOptions =
                SessionCreateParams.PaymentMethodOptions.builder()
                        .setWechatPay(
                                SessionCreateParams.PaymentMethodOptions.WechatPay.builder()
                                        .setClient(WEB)
                                        .build()
                        )
                        .setCard(
                                SessionCreateParams.PaymentMethodOptions.Card.builder()
                                        .setRequestThreeDSecure(
                                                SessionCreateParams.PaymentMethodOptions.Card.RequestThreeDSecure.CHALLENGE)
                                        .build()
                        )
                        .build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(appendSessionIdToUrl(request.getSuccessUrl()))
                .setCancelUrl(request.getCancelUrl())
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.ALIPAY)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.WECHAT_PAY)
                .setPaymentMethodOptions(paymentMethodOptions)
                .setExpiresAt(Instant.now().plusSeconds(30 * 60).getEpochSecond())
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
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("bookingRefNo", booking.getRefNo())
                                .putMetadata("userSub", userSub)
                                .build()
                )
                .putMetadata("bookingRefNo", booking.getRefNo())
                .putMetadata("userSub", userSub)
                .build();
        try {
            Session session = stripeClient.v1().checkout().sessions().create(params);

            booking.setStatus(Enums.BookingStatus.AWAITING_PAYMENT);
            bookingsRepository.save(booking);

            return session.getUrl();
        } catch(StripeException e) {
            throw new CreateSessionException("Failed to create a session for payment");
        }
    }

    public GetPaymentDetailsResponseDTO getPaymentDetails(String sessionId) {
        Payments payment = paymentsRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        Bookings booking = bookingsRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", payment.getBookingId())));

        return GetPaymentDetailsResponseDTO.builder()
                .message("Payment details retrieved successfully")
                .bookingId(booking.getRefNo())
                .paymentId(payment.getRefNo())
                .paymentPlatform(payment.getPaymentPlatform())
                .paymentChannel(payment.getPaymentChannel())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentStatus(payment.getPaymentStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }

    @Transactional
    public RefundResponseDTO refundBooking(RefundRequestDTO requestDTO) {
        Bookings booking = bookingsRepository.findByRefNo(requestDTO.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", requestDTO.getBookingId())));

        if (refundsRepository.findByBookingIdAndSuccessOrProcessingStatus(booking.getId()) != null) {
            throw new AlreadyRefundedException("This payment has already been fully refunded/processing refund");
        }

        if(!booking.getCurrency().equals(requestDTO.getRefundCurrency())) {
            throw new MismatchedCurrencyException("Invalid currency for this refund");
        }

        Refunds r = new Refunds();
        r.setRefNo(referenceNoGenerator.generateRefundReference());
        r.setBookingId(booking.getId());
        r.setCurrency(requestDTO.getRefundCurrency());
        r.setAmount(requestDTO.getRefundAmount());
        r.setRemarks(requestDTO.getRemarks());

        if(requestDTO.getIsFullRefund() && booking.getType().equals(Enums.BookingType.ONLINE_PAYMENT)) {
            if (booking.getFinalPaidAmount().compareTo(requestDTO.getRefundAmount()) != 0) {
                throw new RuntimeException("Invalid amount for full amount");
            }
            Payments payment = paymentsRepository.findByBookingIdAndPaymentStatus(booking.getId(), Enums.PaymentStatus.SUCCEEDED)
                    .orElseThrow(() -> new PaymentNotFoundException(String.format("Payment not found with booking ID %s", booking.getId())));
            RefundCreateParams.Builder refundParams = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getPaymentIntentId())
                    .setAmount(payment.getAmount().longValue() * 100)
                    .putMetadata("bookingRefNo", booking.getRefNo())
                    .putMetadata("paymentIntentId", payment.getPaymentIntentId());
            try {
                Refund refund = stripeClient.v1().refunds().create(refundParams.build());
            } catch(StripeException e) {
                throw new CreateRefundException("Failed to create refund");
            }

            r.setType(Enums.RefundType.ONLINE_REFUND);
            r.setStatus(Enums.RefundStatus.PENDING);
            refundsRepository.save(r);
        } else if (booking.getType().equals(Enums.BookingType.OFFLINE_PAYMENT)) { // Offline Payment with Offline Refund
            booking.setStatus(Enums.BookingStatus.REFUNDED);
            bookingsRepository.save(booking);

            r.setType(Enums.RefundType.OFFLINE_REFUND);
            r.setStatus(Enums.RefundStatus.SUCCESS);
            refundsRepository.save(r);
        } else { // Online Payment but Offline Refund
            booking.setStatus(Enums.BookingStatus.REFUNDED);
            bookingsRepository.save(booking);
            Payments payment = paymentsRepository.findByBookingIdAndPaymentStatus(booking.getId(), Enums.PaymentStatus.SUCCEEDED)
                    .orElseThrow(() -> new PaymentNotFoundException(String.format("Payment not found with booking ID %s", booking.getId())));
            payment.setPaymentStatus(Enums.PaymentStatus.REFUNDED);
            paymentsRepository.save(payment);

            r.setType(Enums.RefundType.OFFLINE_REFUND);
            r.setStatus(Enums.RefundStatus.SUCCESS);
            refundsRepository.save(r);
        }

        return RefundResponseDTO.builder()
                .id(r.getRefNo())
                .refundType(r.getType())
                .refundAmount(r.getAmount())
                .refundCurrency(r.getCurrency())
                .status(r.getStatus())
                .remarks(r.getRemarks())
                .build();
    }

    @Transactional
    public Payments findOrCreatePaymentByPaymentIntentId(String sessionId, String intentId, Bookings booking) {
        if(intentId == null) {
            return createNewPaymentRecord(sessionId, null, booking);
        } else {
            return paymentsRepository.findByPaymentIntentId(intentId)
                    .orElseGet(() -> {
                        return createNewPaymentRecord(sessionId, intentId, booking);
                    });
        }
    }

    @Transactional
    private Payments createNewPaymentRecord(String sessionId, String intentId, Bookings booking) {
        Payments payments = Payments.builder()
                .refNo(referenceNoGenerator.generatePaymentReference())
                .bookingId(booking.getId())
                .amount(booking.getFinalPaidAmount())
                .currency(booking.getCurrency())
                .paymentPlatform(STRIPE)
                .paymentIntentId(intentId)
                .sessionId(sessionId)
                .paymentStatus(Enums.PaymentStatus.INITIATED)
                .build();
        return paymentsRepository.save(payments);
    }

    private String appendSessionIdToUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("Success URL is required");
        }

        String cleanUrl = baseUrl.strip();

        if (cleanUrl.endsWith("/")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
        }

        String separator = cleanUrl.contains("?") ? "&" : "?";

        return cleanUrl + separator + "session_id={CHECKOUT_SESSION_ID}";
    }
}
