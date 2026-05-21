package com.example.exception.booking;

import com.example.exception.BusinessException;

public class EventTimeSlotUnavailableException extends BusinessException {
    public EventTimeSlotUnavailableException(String message) {
        super("BT310", message);
    }
}