package com.example.model.record;

import java.math.BigDecimal;

public record EventBookingSummary(
        BigDecimal totalBooked,
        BigDecimal totalCheckedIn
) {}
