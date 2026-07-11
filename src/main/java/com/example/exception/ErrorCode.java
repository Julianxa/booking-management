package com.example.exception;

import org.springframework.http.HttpStatus;

public class ErrorCode {
    // General
    public static final ErrorDefinition FILE_OP_ERROR =
            new ErrorDefinition("BT001", HttpStatus.BAD_REQUEST, "File operation is failed");

    public static final ErrorDefinition FILE_UPLOAD_ERROR =
            new ErrorDefinition("BT002", HttpStatus.BAD_REQUEST, "File uploading is failed");

    public static final ErrorDefinition HASH_GENERATION_ERROR =
            new ErrorDefinition("BT003", HttpStatus.BAD_REQUEST, "Failed to generate hash");

    public static final ErrorDefinition QR_CODE_GENERATION_ERROR =
            new ErrorDefinition("BT004", HttpStatus.BAD_REQUEST, "Failed to generate QR code");

    public static final ErrorDefinition INTERNAL_SERVER_ERROR =
            new ErrorDefinition("BT005", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    public static final ErrorDefinition INVALID_JSON_FORMAT =
            new ErrorDefinition("BT006", HttpStatus.BAD_REQUEST, "Invalid Json format provided");

    public static final ErrorDefinition MISSING_REQUIRED_FIELD =
            new ErrorDefinition("BT007", HttpStatus.BAD_REQUEST, "Missing required parameter(s)");

    public static final ErrorDefinition PARSE_TOKEN_ERROR =
            new ErrorDefinition("BT008", HttpStatus.NOT_FOUND, "Failed to parse JWT token");

    public static final ErrorDefinition RANDOM_REF_NO_ERROR =
            new ErrorDefinition("BT009", HttpStatus.BAD_REQUEST, "Failed to generate Random Reference number generation");

    // Booking
    public static final ErrorDefinition BOOKING_EVENT_NOT_FOUND =
            new ErrorDefinition("BT100", HttpStatus.NOT_FOUND, "Booking event not found");

    public static final ErrorDefinition BOOKING_FULL =
            new ErrorDefinition("BT101", HttpStatus.CONFLICT, "This slot is fully booked");

    public static final ErrorDefinition BOOKING_NOT_FOUND =
            new ErrorDefinition("BT102", HttpStatus.NOT_FOUND, "Booking failed");

    public static final ErrorDefinition SLOT_UNAVAILABLE =
            new ErrorDefinition("BT103", HttpStatus.CONFLICT, "Time slot is not available");

    public static final ErrorDefinition TICKET_QUANTITY_MISMATCHED =
            new ErrorDefinition("BT104", HttpStatus.CONFLICT, "Ticket quantity mismatched");

    public static final ErrorDefinition THRESHOLD_EXCEEDED =
            new ErrorDefinition("BT105", HttpStatus.BAD_REQUEST, "Booking is not allowed. This event has a threshold.");
    // Email
    public static final ErrorDefinition EMAIL_PROCESSING_ERROR =
            new ErrorDefinition("BT200", HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process email content");

    public static final ErrorDefinition EMAIL_TEMPLATE_NOT_FOUND =
            new ErrorDefinition("BT201", HttpStatus.NOT_FOUND, "Email template not found");

    public static final ErrorDefinition INTERVAL_NOT_FOUND =
            new ErrorDefinition("BT202", HttpStatus.NOT_FOUND, "Missing interval in Reminder email template");
    public static final ErrorDefinition UNVERIFIED_EMAIL =
            new ErrorDefinition("BT202", HttpStatus.NOT_FOUND, "Email is not verified");

    public static final ErrorDefinition OFFICIAL_TEMPLATE =
            new ErrorDefinition("BT203", HttpStatus.BAD_REQUEST, "Official template cannot be deleted");

    public static final ErrorDefinition EMAIL_TEMPLATE_NAME_EXISTS =
            new ErrorDefinition("BT204", HttpStatus.BAD_REQUEST, "Email template name already exists");
    // Event
    public static final ErrorDefinition CAPACITY_EXCEEDED =
            new ErrorDefinition("BT300", HttpStatus.BAD_REQUEST, "Event capacity is exceeded");

    public static final ErrorDefinition SCHEDULE_NOT_FOUND =
            new ErrorDefinition("BT301", HttpStatus.NOT_FOUND, "The day is not in the schedule of event");

    public static final ErrorDefinition EVENT_NOT_FOUND =
            new ErrorDefinition("BT302", HttpStatus.NOT_FOUND, "Event not found");

    // Gift Certificate
    public static final ErrorDefinition GC_ITEM_NOT_FOUND =
            new ErrorDefinition("BT400", HttpStatus.NOT_FOUND, "Gift Certificate Item not found");

    public static final ErrorDefinition GC_NOT_FOUND =
            new ErrorDefinition("BT401", HttpStatus.NOT_FOUND, "Gift Certificate not found");

    public static final ErrorDefinition GC_PROMO_CODE_EXISTS =
            new ErrorDefinition("BT402", HttpStatus.BAD_REQUEST, "Promotion code already exists");

    public static final ErrorDefinition GC_REDEMPTION_NOT_FOUND =
            new ErrorDefinition("BT403", HttpStatus.NOT_FOUND, "Gift Certificate redemption not found");

    public static final ErrorDefinition INVALID_GC =
            new ErrorDefinition("BT404", HttpStatus.BAD_REQUEST, "Invalid Gift Certificate");

    // Organization
    public static final ErrorDefinition ORG_NOT_FOUND =
            new ErrorDefinition("BT500", HttpStatus.NOT_FOUND, "Organization not found");

    // Payment
    public static final ErrorDefinition REFUNDED_ALREADY =
            new ErrorDefinition("BT600", HttpStatus.BAD_REQUEST, "Already refunded");

    public static final ErrorDefinition CREATE_REFUND_ERROR =
            new ErrorDefinition("BT601", HttpStatus.BAD_REQUEST, "Failed to create refund");

    public static final ErrorDefinition CREATE_SESSION_ERROR =
            new ErrorDefinition("BT602", HttpStatus.BAD_REQUEST, "Failed to create session");

    public static final ErrorDefinition CURRENCY_MISMATCHED =
            new ErrorDefinition("BT603", HttpStatus.BAD_REQUEST, "Mismatched currency provided");

    public static final ErrorDefinition PAYMENT_NOT_FOUND =
            new ErrorDefinition("BT604", HttpStatus.NOT_FOUND, "Payment not found");

    public static final ErrorDefinition PAYMENT_PROCESSING_ERROR =
            new ErrorDefinition("BT605", HttpStatus.BAD_REQUEST, "Payment processing error");

    public static final ErrorDefinition REFUND_NOT_FOUND =
            new ErrorDefinition("BT606", HttpStatus.NOT_FOUND, "Refund not found");
    // Ticket
    public static final ErrorDefinition INVALID_VERIFICATION_TOKEN =
            new ErrorDefinition("BT700", HttpStatus.BAD_REQUEST, "Invalid verification token");

    public static final ErrorDefinition TICKET_PRICE_NOT_FOUND =
            new ErrorDefinition("BT701", HttpStatus.NOT_FOUND, "Ticket price not found");

    public static final ErrorDefinition TICKET_TYPE_NOT_FOUND =
            new ErrorDefinition("BT702", HttpStatus.NOT_FOUND, "Ticket type not found");

    // User
    public static final ErrorDefinition INVALID_ACCCESS_TOKEN =
            new ErrorDefinition("BT803", HttpStatus.NOT_FOUND, "Invalid access token");

    public static final ErrorDefinition INVALID_EMAIL_PASSWORD =
            new ErrorDefinition("BT804", HttpStatus.NOT_FOUND, "Invalid email or password");

    public static final ErrorDefinition INVALID_ID_TOKEN =
            new ErrorDefinition("BT805", HttpStatus.NOT_FOUND, "Invalid ID token");

    public static final ErrorDefinition USER_NOT_FOUND =
            new ErrorDefinition("BT806", HttpStatus.NOT_FOUND, "User not found");

    // Report
    public static final ErrorDefinition REPORT_NOT_FOUND =
            new ErrorDefinition("BT901", HttpStatus.NOT_FOUND, "Report not found");

}