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

    public static boolean hasConfiguredThreshold(Events event) {
        return isConfigured(event.getActivityHourThreshold())
                || isConfigured(event.getActivityDayThreshold());
    }

    /**
     * Returns true when the event is still upcoming and the current time is inside the
     * activity booking cutoff window (the same window that blocks new online bookings).
     */
    public static boolean isWithinActivityThreshold(
            Events event,
            LocalDate eventDate,
            LocalTime eventTime,
            ZonedDateTime now) {
        ZonedDateTime eventStartTime = ZonedDateTime.of(eventDate, eventTime, ZoneId.systemDefault());
        if (!eventStartTime.isAfter(now)) {
            return false;
        }

        if (isConfigured(event.getActivityHourThreshold())) {
            long minutesUntilEvent = ChronoUnit.MINUTES.between(now, eventStartTime);
            long requiredMinutes = event.getActivityHourThreshold() * 60L;
            return minutesUntilEvent <= requiredMinutes;
        }

        if (isConfigured(event.getActivityDayThreshold())) {
            long daysUntilEvent = ChronoUnit.DAYS.between(now.toLocalDate(), eventDate);
            return daysUntilEvent <= event.getActivityDayThreshold();
        }

        return false;
    }

    /**
     * Reminder resend is allowed once the activity threshold window is reached, or immediately
     * when no activity threshold is configured on the event.
     */
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

        if (!hasConfiguredThreshold(event)) {
            return true;
        }

        return isWithinActivityThreshold(event, eventDate, parsedEventTime, now);
    }
}
