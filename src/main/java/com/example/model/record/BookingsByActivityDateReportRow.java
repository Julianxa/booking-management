package com.example.model.record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public record BookingsByActivityDateReportRow(
        String bookingRefNo,
        Long bookingEventId,
        LocalDate eventDate,
        String eventTime,
        BigDecimal eventSubtotal,
        ZonedDateTime purchaseDate,
        BigDecimal bookingDiscount,
        BigDecimal bookingTotalPaidPrice,
        BigDecimal bookingFinalPaidAmount,
        Long giftCertificateId,
        String eventType,
        String eventName,
        String eventNameZhHk,
        String eventCategory,
        String guestFirstName,
        String guestLastName,
        String guestCountry,
        String userFirstName,
        String userLastName,
        String userRole,
        String organizationName,
        String companyType,
        String companyGroup
) {
}
