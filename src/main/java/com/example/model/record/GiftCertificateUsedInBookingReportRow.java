package com.example.model.record;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record GiftCertificateUsedInBookingReportRow(
    String bookingRefNo,
    ZonedDateTime purchaseDateTime,
    String giftCertificateRefNo,
    String recipient,
    String messageToRecipient,
    BigDecimal discount) {}
