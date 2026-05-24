package com.example.exception.event;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.SCHEDULE_NOT_FOUND;

public class EventDayScheduleNotFoundException extends BusinessException {
    public EventDayScheduleNotFoundException() {
        super(SCHEDULE_NOT_FOUND);
    }
    public EventDayScheduleNotFoundException(String message) {
        super(SCHEDULE_NOT_FOUND, message);
    }
}
