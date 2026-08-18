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
            case REFUNDED -> false;
            case EXPIRED -> newStatus == Enums.PaymentStatus.SUCCEEDED;
            case SUCCEEDED -> newStatus == Enums.PaymentStatus.REFUNDED;
            case PENDING, INITIATED, REQUIRES_ACTION, FAILED ->
                    newStatus == Enums.PaymentStatus.INITIATED ||
                            newStatus == Enums.PaymentStatus.REQUIRES_ACTION ||
                            newStatus == Enums.PaymentStatus.SUCCEEDED ||
                            newStatus == Enums.PaymentStatus.FAILED ||
                            newStatus == Enums.PaymentStatus.EXPIRED;
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
            case CANCELLED ->
                    newStatus == Enums.BookingStatus.CONFIRMED
                            || newStatus == Enums.BookingStatus.PAID
                            || newStatus == Enums.BookingStatus.REFUNDED;
            case REFUNDED -> false;
            case EXPIRED, FAILED -> newStatus == Enums.BookingStatus.REFUNDED;

            case ON_HOLD ->
                    newStatus == Enums.BookingStatus.AWAITING_PAYMENT ||
                            newStatus == Enums.BookingStatus.PAYMENT_IN_PROGRESS ||
                            newStatus == Enums.BookingStatus.PAID ||
                            newStatus == Enums.BookingStatus.CONFIRMED ||
                            newStatus == Enums.BookingStatus.FAILED ||
                            newStatus == Enums.BookingStatus.CANCELLED ||
                            newStatus == Enums.BookingStatus.EXPIRED;

            case AWAITING_PAYMENT ->
                    newStatus == Enums.BookingStatus.PAYMENT_IN_PROGRESS ||
                            newStatus == Enums.BookingStatus.PAID ||
                            newStatus == Enums.BookingStatus.CONFIRMED ||
                            newStatus == Enums.BookingStatus.FAILED ||
                            newStatus == Enums.BookingStatus.CANCELLED ||
                            newStatus == Enums.BookingStatus.EXPIRED;

            case PAYMENT_IN_PROGRESS ->
                    newStatus == Enums.BookingStatus.PAID ||
                            newStatus == Enums.BookingStatus.CONFIRMED ||
                            newStatus == Enums.BookingStatus.FAILED ||
                            newStatus == Enums.BookingStatus.CANCELLED ||
                            newStatus == Enums.BookingStatus.EXPIRED;

            case PAID ->
                    newStatus == Enums.BookingStatus.CONFIRMED ||
                            newStatus == Enums.BookingStatus.FAILED ||
                            newStatus == Enums.BookingStatus.CANCELLED ||
                            newStatus == Enums.BookingStatus.EXPIRED;

            case CONFIRMED ->
                    newStatus == Enums.BookingStatus.CANCELLED ||
                            newStatus == Enums.BookingStatus.REFUNDED;

            default -> true;
        };
    }

    public boolean shouldUpdateRefundStatus(Enums.RefundStatus current, Enums.RefundStatus newStatus) {
        if (current == null) {
            return true;
        }

        if (current == newStatus) {
            return false;
        }

        return switch (current) {
            case FAILED, SUCCESS -> false;

            case PENDING ->
                    newStatus == Enums.RefundStatus.PROCESSING ||
                            newStatus == Enums.RefundStatus.SUCCESS ||
                            newStatus == Enums.RefundStatus.FAILED;

            case PROCESSING ->
                    newStatus == Enums.RefundStatus.SUCCESS ||
                            newStatus == Enums.RefundStatus.FAILED;

            default -> true;
        };
    }
}
