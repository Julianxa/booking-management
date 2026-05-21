package com.example.exception.bookingEvent;

import com.example.exception.BusinessException;

public class BookingEventNotFoundException extends BusinessException {
    public BookingEventNotFoundException(String message) {
        super("BT401", message);
    }
}
