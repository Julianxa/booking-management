package com.example.exception.booking;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.BOOKING_FULL;

public class BookingFullException extends BusinessException {
    public BookingFullException() {
        super(BOOKING_FULL);
    }
    public BookingFullException(String message) {
        super(BOOKING_FULL, message);
    }
}