package com.example.exception.booking;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.BOOKING_NOT_FOUND;

public class BookingNotFoundException extends BusinessException {
    public BookingNotFoundException() {
        super(BOOKING_NOT_FOUND);
    }
    public BookingNotFoundException(String message) {
        super(BOOKING_NOT_FOUND, message);
    }
}
