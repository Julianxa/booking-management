package com.example.exception.booking;

import com.example.exception.BusinessException;

public class InvalidBookingStateException extends BusinessException {
    public InvalidBookingStateException(String message) {
        super("BT305", message);
    }
}
