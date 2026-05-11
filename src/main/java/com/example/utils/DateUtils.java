package com.example.utils;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;

@Component
public class DateUtils {
    public String getDayValueForDate(LocalDate date) {
        if (date == null) return null;

        return date.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                .toUpperCase();   // Returns "MON", "TUE", "WED"
    }

    public LocalDateTime convertToLocalDateTime(Long timestampInSeconds) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestampInSeconds),
                ZoneId.systemDefault()
        );
    }
}
