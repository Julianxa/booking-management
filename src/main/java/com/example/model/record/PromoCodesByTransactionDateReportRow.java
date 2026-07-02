package com.example.model.record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public record PromoCodesByTransactionDateReportRow(
    String bookingRefNo,
    Long bookingEventId,
    ZonedDateTime transactionDateTime,
    LocalDate eventDate,
    String eventTime,
    String eventName,
    String eventNameZhHk,
    String guestFirstName,
    String guestLastName,
    BigDecimal eventSubtotal,
    String promoCode,
    String giftCertificateType,
    BigDecimal promoItemValue,
    BigDecimal bookingDiscount,
    BigDecimal bookingTotalPaidPrice) {}
