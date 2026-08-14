package com.example.exception.booking;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.BOOKING_EVENT_ALREADY_CANCELLED;

public class BookingEventAlreadyCancelledException extends BusinessException {
    public BookingEventAlreadyCancelledException() {
        super(BOOKING_EVENT_ALREADY_CANCELLED);
    }

    public BookingEventAlreadyCancelledException(String message) {
        super(BOOKING_EVENT_ALREADY_CANCELLED, message);
    }
}
