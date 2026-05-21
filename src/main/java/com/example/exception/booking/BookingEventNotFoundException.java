package com.example.exception.booking;

import com.example.exception.BusinessException;

public class BookingEventNotFoundException extends BusinessException {
    public BookingEventNotFoundException(String message) {
        super("BT301", message);
    }
}
