package com.example.model.record;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpiredGiftCertificateCodesReportRow(
    String typeLabel,
    String giftCertificateRefNo,
    String promoCode,
    String statusLabel,
    BigDecimal wholesaleValue,
    BigDecimal retailValue,
    LocalDate expiryDate,
    String purchaserName,
    String description,
    BigDecimal balance) {}
