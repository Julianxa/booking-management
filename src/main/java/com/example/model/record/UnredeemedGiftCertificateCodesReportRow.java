package com.example.model.record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public record UnredeemedGiftCertificateCodesReportRow(
    String typeLabel,
    String giftCertificateRefNo,
    String promoCode,
    String statusLabel,
    BigDecimal wholesaleValue,
    BigDecimal retailValue,
    ZonedDateTime dateIssued,
    LocalDate expiryDate,
    String purchaserName,
    String description,
    BigDecimal balance) {}
