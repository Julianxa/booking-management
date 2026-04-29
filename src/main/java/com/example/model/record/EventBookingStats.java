package com.example.model.record;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EventBookingStats(
        String eventId,
        String eventName,
        LocalDate eventDate,
        String scheduleDay,
        String eventTime,
        Integer maxCapacity,
        Integer totalBooked,
        Integer totalCheckedIn,
        BigDecimal bookingPercentage,
        BigDecimal checkInPercentage
) {}