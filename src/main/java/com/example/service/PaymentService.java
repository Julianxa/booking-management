package com.example.service;


import com.example.constant.Enums;
import com.example.exception.booking.BookingNotFoundException;
import com.example.exception.payment.*;
import com.example.mapper.RefundMapper;
import com.example.model.dto.*;
import com.example.model.entity.Bookings;
import com.example.model.entity.Payments;
import com.example.model.entity.Refunds;
import com.example.repository.BookingsRepository;
import com.example.repository.PaymentsRepository;
import com.example.repository.RefundsRepository;
import com.example.utils.ReferenceNoGenerator;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static com.stripe.param.checkout.SessionCreateParams.PaymentMethodOptions.WechatPay.Client.WEB;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final AuditService auditService;
    private final StripeClient stripeClient;
    private final PaymentsRepository paymentsRepository;
    private final BookingsRepository bookingsRepository;
    private final RefundsRepository refundsRepository;
    private final ReferenceNoGenerator referenceNoGenerator;
    private final RefundMapper refundMapper;
    private final PaymentLogService paymentLogService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Session createCheckoutSession(String userSub, CreateBookingRequestDTO request, Bookings booking) {
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

            return session;
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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public RefundResponseDTO refundBooking(RefundRequestDTO requestDTO) {
        Bookings booking = bookingsRepository.findByRefNo(requestDTO.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", requestDTO.getBookingId())));

        if (booking.getStatus() != Enums.BookingStatus.CANCELLED
                && booking.getStatus() != Enums.BookingStatus.EXPIRED
                && booking.getStatus() != Enums.BookingStatus.FAILED) {
            throw new IllegalArgumentException(
                    "Booking must be cancelled, expired, or failed before a refund can be processed");
        }

        if (refundsRepository.findActiveByBookingId(booking.getId()).isPresent()) {
            throw new AlreadyRefundedException("This payment has already been fully refunded or a refund is in progress");
        }

        if (requestDTO.getRefundCurrency() == null
                || !booking.getCurrency().equals(requestDTO.getRefundCurrency())) {
            throw new MismatchedCurrencyException("Invalid currency for this refund");
        }

        Payments capturedPayment = null;
        if (booking.getType().equals(Enums.BookingType.ONLINE_PAYMENT)) {
            capturedPayment = paymentsRepository
                    .findByBookingIdAndPaymentStatus(booking.getId(), Enums.PaymentStatus.SUCCEEDED)
                    .orElseThrow(() -> new PaymentNotFoundException(
                            String.format(
                                    "No captured payment to refund for booking %s. Expired or failed bookings can only be refunded if Stripe later succeeded.",
                                    booking.getRefNo())));
        }

        Refunds refund = new Refunds();
        refund.setRefNo(referenceNoGenerator.generateRefundReference());
        refund.setBookingId(booking.getId());
        refund.setCurrency(requestDTO.getRefundCurrency());
        refund.setAmount(requestDTO.getRefundAmount());
        refund.setRemarks(requestDTO.getRemarks());

        if (Boolean.TRUE.equals(requestDTO.getIsFullRefund())
                && booking.getType().equals(Enums.BookingType.ONLINE_PAYMENT)) {
            if (booking.getFinalPaidAmount().compareTo(requestDTO.getRefundAmount()) != 0) {
                throw new RuntimeException("Invalid amount for full amount");
            }

            refund.setType(Enums.RefundType.ONLINE_REFUND);
            refund.setStatus(Enums.RefundStatus.PENDING);
            refundsRepository.save(refund);

            RefundCreateParams.Builder refundParams = RefundCreateParams.builder()
                    .setPaymentIntent(capturedPayment.getPaymentIntentId())
                    .setAmount(capturedPayment.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact())
                    .putMetadata("bookingRefNo", booking.getRefNo())
                    .putMetadata("paymentIntentId", capturedPayment.getPaymentIntentId());
            try {
                stripeClient.v1().refunds().create(refundParams.build());
            } catch (StripeException e) {
                refund.setStatus(Enums.RefundStatus.FAILED);
                refundsRepository.save(refund);
                throw new CreateRefundException("Failed to create refund");
            }
        } else if (booking.getType().equals(Enums.BookingType.OFFLINE_PAYMENT)) { // Offline Payment with Offline Refund
            finalizeOfflineRefund(booking, refund, Enums.RefundType.OFFLINE_REFUND, null);
        } else { // Online Payment but Offline Refund
            finalizeOfflineRefund(booking, refund, Enums.RefundType.OFFLINE_REFUND, capturedPayment);
        }

        auditService.record("REFUND_BOOKING",
                Refunds.class.getName(),
                refund.getId(),
                null,
                "Refund booking successfully"
        );
        return refundMapper.toCreateResponseDTO(booking.getRefNo(), refund);
    }

    public GetListRefundResponseDTO getRefunds(String bookingRefNo) {
        Bookings booking = bookingsRepository.findByRefNo(bookingRefNo)
                .orElseThrow(() -> new BookingNotFoundException(String.format("Booking %s not found", bookingRefNo)));
        List<Refunds> refunds = refundsRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new RefundNotFoundException("Refund not found"));

        List<RefundResponseDTO> content = refunds.stream()
                .map(refund -> {
                    RefundResponseDTO refundResponseDTO = refundMapper.toCreateResponseDTO(bookingRefNo, refund);
                    refundResponseDTO.setStatus(refund.getStatus());
                    return refundResponseDTO;
                })
                .toList();

        GetListRefundResponseDTO getListRefundResponseDTO = new GetListRefundResponseDTO();
        getListRefundResponseDTO.setContent(content);
        getListRefundResponseDTO.setMessage("Retrieve Refund history successfully");
        getListRefundResponseDTO.setTimestamp(ZonedDateTime.now());
        return getListRefundResponseDTO;
    }

    public GetListRefundResponseDTO getAllRefunds(Pageable pageable) {
        Page<Refunds> refundsPage = refundsRepository.findAll(pageable);

        List<RefundResponseDTO> content = refundsPage.getContent().stream()
                .map(refund -> {
                    String bookingRefNo = bookingsRepository.findRefNoById(refund.getBookingId())
                            .orElseThrow(() -> new BookingNotFoundException("Booking not found"));
                    RefundResponseDTO refundResponseDTO = refundMapper.toCreateResponseDTO(bookingRefNo, refund);
                    refundResponseDTO.setStatus(refund.getStatus());
                    return refundResponseDTO;
                })
                .toList();

        GetListRefundResponseDTO getListRefundResponseDTO = refundMapper.toGetListResponse(refundsPage, content);
        getListRefundResponseDTO.setMessage("Retrieve list of Events successfully.");
        getListRefundResponseDTO.setTimestamp(ZonedDateTime.now());
        return getListRefundResponseDTO;
    }

    @Transactional
    public Payments findOrCreatePaymentByPaymentIntentId(String sessionId, String intentId, Enums.PaymentPlatform paymentPlatform, Bookings booking) {
        if (sessionId != null)
            return paymentsRepository.findBySessionId(sessionId)
                    .orElseGet(() -> {
                        return createNewPaymentRecord(sessionId, null, paymentPlatform, booking);
                    });
        else
            return paymentsRepository.findByBookingId(booking.getId())
                    .orElseGet(() -> {
                        return createNewPaymentRecord(null, null, paymentPlatform, booking);
                    });
    }

    @Transactional
    private Payments createNewPaymentRecord(String sessionId, String intentId, Enums.PaymentPlatform paymentPlatform, Bookings booking) {
        Payments payments = Payments.builder()
                .refNo(referenceNoGenerator.generatePaymentReference())
                .bookingId(booking.getId())
                .amount(booking.getFinalPaidAmount())
                .currency(booking.getCurrency())
                .paymentPlatform(paymentPlatform)
                .paymentIntentId(intentId)
                .sessionId(sessionId)
                .paymentStatus(Enums.PaymentStatus.PENDING)
                .build();
        Payments saved = paymentsRepository.save(payments);
        paymentLogService.recordStatusChange(saved, Enums.PaymentStatus.PENDING, null, null);
        return saved;
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

    private void finalizeOfflineRefund(Bookings booking, Refunds refund, Enums.RefundType refundType, Payments payment) {
        booking.setStatus(Enums.BookingStatus.REFUNDED);
        bookingsRepository.save(booking);

        if (payment != null) {
            payment.setPaymentStatus(Enums.PaymentStatus.REFUNDED);
            paymentsRepository.save(payment);
            paymentLogService.recordStatusChange(
                    payment,
                    Enums.PaymentStatus.REFUNDED,
                    null,
                    payment.getPaymentChannel() != null ? payment.getPaymentChannel().name().toLowerCase() : null);
        }

        refund.setType(refundType);
        refund.setStatus(Enums.RefundStatus.SUCCESS);
        refundsRepository.save(refund);
    }
}
