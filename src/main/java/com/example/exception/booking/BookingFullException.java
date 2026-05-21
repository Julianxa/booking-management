package com.example.exception.booking;

import com.example.exception.BusinessException;

public class BookingFullException extends BusinessException {
    public BookingFullException(String message) {
        super("BT302", message);
    }
}