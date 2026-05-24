package com.example.exception.booking;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.BOOKING_EVENT_NOT_FOUND;

public class BookingEventNotFoundException extends BusinessException {
    public BookingEventNotFoundException() {
        super(BOOKING_EVENT_NOT_FOUND);
    }
    public BookingEventNotFoundException(String message) {
        super(BOOKING_EVENT_NOT_FOUND, message);
    }
}
