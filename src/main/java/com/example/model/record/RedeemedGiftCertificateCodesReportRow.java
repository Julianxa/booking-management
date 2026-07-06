package com.example.model.record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public record RedeemedGiftCertificateCodesReportRow(
    String typeLabel,
    String giftCertificateRefNo,
    String promoCode,
    String statusLabel,
    Long bookingId,
    LocalDate dateIssued,
    LocalDate expiryDate,
    ZonedDateTime dateRedeemed,
    long daysToRedeemed,
    String purchaserName,
    String description,
    BigDecimal wholesaleValue,
    BigDecimal retailValue,
    BigDecimal redeemedValue,
    BigDecimal difference) {}
