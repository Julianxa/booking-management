package com.example.model.record;

public record EventTimeSlotException(
        String eventId,
        String eventTime
) {}
