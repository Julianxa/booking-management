package com.example.utils;

import com.example.model.entity.Events;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class ActivityThresholdUtil {

    private ActivityThresholdUtil() {
    }

    public static boolean isConfigured(Integer threshold) {
        return threshold != null && threshold > 0;
    }

    public static Integer normalize(Integer threshold) {
        return isConfigured(threshold) ? threshold : null;
    }

    public static boolean isWithinMinActivityThreshold(
            Events event,
            LocalDate eventDate,
            LocalTime eventTime,
            ZonedDateTime now) {
        ZonedDateTime eventStartTime = ZonedDateTime.of(eventDate, eventTime, ZoneId.systemDefault());
        if (!eventStartTime.isAfter(now)) {
            return false;
        }

        if (isConfigured(event.getMinActivityHourThreshold())) {
            long minutesUntilEvent = ChronoUnit.MINUTES.between(now, eventStartTime);
            long requiredMinutes = event.getMinActivityHourThreshold() * 60L;
            return minutesUntilEvent <= requiredMinutes;
        }

        if (isConfigured(event.getMinActivityDayThreshold())) {
            long daysUntilEvent = ChronoUnit.DAYS.between(now.toLocalDate(), eventDate);
            return daysUntilEvent <= event.getMinActivityDayThreshold();
        }

        return false;
    }

    public static boolean isBeyondMaxActivityThreshold(
            Events event,
            LocalDate eventDate,
            LocalTime eventTime,
            ZonedDateTime now) {
        ZonedDateTime eventStartTime = ZonedDateTime.of(eventDate, eventTime, ZoneId.systemDefault());
        if (!eventStartTime.isAfter(now)) {
            return false;
        }

        if (isConfigured(event.getMaxActivityHourThreshold())) {
            long minutesUntilEvent = ChronoUnit.MINUTES.between(now, eventStartTime);
            long allowedMinutes = event.getMaxActivityHourThreshold() * 60L;
            return minutesUntilEvent > allowedMinutes;
        }

        if (isConfigured(event.getMaxActivityDayThreshold())) {
            long daysUntilEvent = ChronoUnit.DAYS.between(now.toLocalDate(), eventDate);
            return daysUntilEvent > event.getMaxActivityDayThreshold();
        }

        return false;
    }

    public static boolean isActivityThresholdAttainedForReminder(
            Events event,
            LocalDate eventDate,
            String eventTime,
            ZonedDateTime now) {
        LocalTime parsedEventTime = LocalTime.parse(eventTime);
        ZonedDateTime eventStartTime = ZonedDateTime.of(eventDate, parsedEventTime, ZoneId.systemDefault());
        if (!eventStartTime.isAfter(now)) {
            return false;
        }

        Integer reminderDayInterval = event.getEmailTemplate() != null
                ? event.getEmailTemplate().getReminderDayInterval()
                : null;
        if (!isConfigured(reminderDayInterval)) {
            return true;
        }

        long daysUntilEvent = ChronoUnit.DAYS.between(now.toLocalDate(), eventDate);
        return daysUntilEvent <= reminderDayInterval;
    }
}
