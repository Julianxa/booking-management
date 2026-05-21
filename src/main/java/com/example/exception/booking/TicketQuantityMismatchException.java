package com.example.exception.booking;

import com.example.exception.BusinessException;

public class TicketQuantityMismatchException extends BusinessException {
    public TicketQuantityMismatchException(String message) {
        super("BT307", message);
    }
}
