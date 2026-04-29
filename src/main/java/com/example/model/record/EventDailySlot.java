package com.example.model.record;

public record EventDailySlot(
        Long eventId,
        String eventRef,
        String eventName,
        String scheduleDay,
        String eventDate,
        String eventTime,
        Long maxCapacity
) {}
