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

    public enum BookingStatus {
        SUCCESS,
        FAILED,
        CANCELLED,
        PENDING
    }

    public enum BookingEventStatus {
        CHECKED_IN,
        AVAILABLE,
        NO_SHOW,
        CANCELLED
    }

    public enum PaymentStatus {
        PAID,
        PENDING,
        REFUNDED,
        FAILED
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
        REDEEMED,
        EXPIRED,
        CANCELLED
    }

    public enum Weekday {
        MON, TUE, WED, THU, FRI, SAT, SUN
    }
}
