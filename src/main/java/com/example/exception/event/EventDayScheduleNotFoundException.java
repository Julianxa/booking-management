package com.example.exception.event;

import com.example.exception.BusinessException;

public class EventDayScheduleNotFoundException extends BusinessException {
    public EventDayScheduleNotFoundException(String message) {
        super("BT102", message);
    }
}
