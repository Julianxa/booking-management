package com.example.exception.booking;

import com.example.exception.BusinessException;

public class BookingNotFoundException extends BusinessException {
    public BookingNotFoundException(String message) {
        super("BT303", message);
    }
}
