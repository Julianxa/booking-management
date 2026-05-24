package com.example.exception.booking;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.SLOT_UNAVAILABLE;

public class EventTimeSlotUnavailableException extends BusinessException {
    public EventTimeSlotUnavailableException() {
        super(SLOT_UNAVAILABLE);
    }
    public EventTimeSlotUnavailableException(String message) {
        super(SLOT_UNAVAILABLE, message);
    }
}