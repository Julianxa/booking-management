package com.example.utils;

import com.example.constant.Enums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatusTransitioner {
    public boolean shouldUpdatePaymentStatus(Enums.PaymentStatus current, Enums.PaymentStatus newStatus) {
        if (current == null) {
            return true;
        }

        if (current == newStatus) {
            return false;
        }

        return switch (current) {
            case FAILED, CANCELLED, EXPIRED, REFUNDED -> false;

            case INITIATED ->
                    newStatus == Enums.PaymentStatus.REQUIRES_ACTION ||
                            newStatus == Enums.PaymentStatus.SUCCEEDED ||
                            newStatus == Enums.PaymentStatus.FAILED ||
                            newStatus == Enums.PaymentStatus.CANCELLED ||
                            newStatus == Enums.PaymentStatus.EXPIRED;

            case REQUIRES_ACTION ->
                    newStatus == Enums.PaymentStatus.SUCCEEDED ||
                            newStatus == Enums.PaymentStatus.FAILED ||
                            newStatus == Enums.PaymentStatus.CANCELLED ||
                            newStatus == Enums.PaymentStatus.EXPIRED;

            case SUCCEEDED ->
                    newStatus == Enums.PaymentStatus.REFUNDED ||
                            newStatus == Enums.PaymentStatus.CANCELLED;

            default -> true;
        };
    }

    public boolean shouldUpdateBookingStatus(Enums.BookingStatus current, Enums.BookingStatus newStatus) {
        if (current == null) {
            return true;
        }

        if (current == newStatus) {
            return false;
        }

        return switch (current) {
            case FAILED, CANCELLED, EXPIRED, REFUNDED -> false;

            case PENDING ->
                    newStatus == Enums.BookingStatus.AWAITING_PAYMENT ||
                            newStatus == Enums.BookingStatus.PAYMENT_IN_PROGRESS ||
                            newStatus == Enums.BookingStatus.PAID ||
                            newStatus == Enums.BookingStatus.SUCCESS ||
                            newStatus == Enums.BookingStatus.FAILED ||
                            newStatus == Enums.BookingStatus.CANCELLED ||
                            newStatus == Enums.BookingStatus.EXPIRED;

            case AWAITING_PAYMENT ->
                    newStatus == Enums.BookingStatus.PAYMENT_IN_PROGRESS ||
                            newStatus == Enums.BookingStatus.PAID ||
                            newStatus == Enums.BookingStatus.SUCCESS ||
                            newStatus == Enums.BookingStatus.FAILED ||
                            newStatus == Enums.BookingStatus.CANCELLED ||
                            newStatus == Enums.BookingStatus.EXPIRED;

            case PAYMENT_IN_PROGRESS ->
                    newStatus == Enums.BookingStatus.PAID ||
                            newStatus == Enums.BookingStatus.SUCCESS ||
                            newStatus == Enums.BookingStatus.FAILED ||
                            newStatus == Enums.BookingStatus.CANCELLED ||
                            newStatus == Enums.BookingStatus.EXPIRED;

            case PAID ->
                    newStatus == Enums.BookingStatus.SUCCESS ||
                            newStatus == Enums.BookingStatus.FAILED ||
                            newStatus == Enums.BookingStatus.CANCELLED ||
                            newStatus == Enums.BookingStatus.EXPIRED;

            case SUCCESS ->
                    newStatus == Enums.BookingStatus.CANCELLED ||
                            newStatus == Enums.BookingStatus.REFUNDED;

            default -> true;
        };
    }
}
