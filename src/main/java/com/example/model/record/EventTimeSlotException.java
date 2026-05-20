package com.example.model.record;

import java.util.Date;

public record EventTimeSlotException(
        String eventId,
        Date eventDate,
        String eventTime
) {
}
