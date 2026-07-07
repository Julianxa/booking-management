package com.example.constant;

import com.fasterxml.jackson.annotation.JsonProperty;


public class Enums {
    public enum UserRole {
        @JsonProperty("ADMIN")
        ADMIN,
        @JsonProperty("AGENT")
        AGENT,
        @JsonProperty("EMPLOYEE")
        EMPLOYEE,
        @JsonProperty("USER")
        USER,
    }

    public enum OrganizationStatus {
        ACTIVE,
        INACTIVE
    }

    public enum NotificationStatus {
        PENDING,
        SUCCESS,
        FAILED
    }

    public enum LoginActivityStatus {
        SUCCESS,
        FAILED,
        LOCKED,
        PENDING,
        EXPIRED
    }

    public enum UserStatus {
        UNCONFIRMED,
        CONFIRMED,
        INACTIVE
    }

    public enum BookingEventStatus {
        PENDING,
        AVAILABLE,
        CHECKED_IN,
        NO_SHOW,
        CANCELLED
    }

    public enum BookingStatus {
        PENDING,
        AWAITING_PAYMENT,
        PAYMENT_IN_PROGRESS,
        PAID,
        SUCCESS,
        FAILED,
        CANCELLED,
        EXPIRED,
        REFUNDED
    }

    public enum PaymentStatus {
        PENDING,
        INITIATED,
        REQUIRES_ACTION,
        SUCCEEDED,
        FAILED,
        CANCELLED,
        REFUNDED,
        EXPIRED
    }

    public enum OccupancyStatus {
        AVAILABLE,
        FULL,
        CANCELLED
    }

    public enum EventStatus {
        OPEN,
        CLOSE,
        OPEN_WITH_BOOKINGS,
        CLOSE_WITH_BOOKINGS
    }

    public enum RefundStatus {
        PENDING,
        PROCESSING,
        SUCCESS,
        FAILED
    }

    public enum TicketTypeStatus {
        OPEN,
        CLOSE
    }

    public enum GiftCertificateType {
        VALUE, EVENT, PERSONAL_VALUE, PERSONAL_EVENT
    }

    public enum GiftCertificateStatus {
        ACTIVE,
        CONSUMED,
        EXPIRED,
        CANCELLED
    }

    public enum GiftCertificateRedemptionStatus {
        PENDING,
        SUCCESS,
        FAILED
    }

    public enum Weekday {
        MON, TUE, WED, THU, FRI, SAT, SUN
    }

    public enum PaymentPlatform {
        STRIPE
    }

    public enum PaymentChannel {
        CARD, ALIPAY, WECHAT_PAY
    }

    public enum BookingType {
        OFFLINE_PAYMENT, ONLINE_PAYMENT
    }

    public enum RefundType {
        OFFLINE_REFUND, ONLINE_REFUND
    }

    public enum EmailStatus {
        SUCCESS,
        FAILED,

    }

    public enum Language {
        CN, HK, EN
    }

    public enum BookingEmailType {
        BOOKING_CONFIRMATION, PAYMENT_CONFIRMATION, BOOKING_CANCELLATION, BOOKING_REMINDER
    }

    public enum ReportStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    public enum ReportType {
        BOOKINGS_BY_ACTIVITY_DATE,
        BOOKINGS_BY_PURCHASE_DATE,
        PROMO_CODES_BY_TRANSACTION_DATE,
        COUNTRY_OF_ORIGIN,
        EXPIRED_GIFT_CERTIFICATE_CODES,
        REDEEMED_GIFT_CERTIFICATE_CODES,
        UNREDEEMED_GIFT_CERTIFICATE_CODES,
        ALL_BOOKINGS
    }
}
