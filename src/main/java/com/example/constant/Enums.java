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
        PAYMENT_PENDING,
        PAYMENT_IN_PROGRESS,
        PAID,
        SUCCESS,
        FAILED,
        CANCELLED,
        EXPIRED
    }

    public enum PaymentStatus {
        PENDING,
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

    public enum TicketTypeStatus {
        OPEN,
        CLOSE
    }

    public enum GiftCertificateType {
        VALUE, EVENT
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
        CARD, ALIPAY, WECHATPAY
    }
}
