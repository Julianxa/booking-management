package com.example.exception.booking;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.TICKET_QUANTITY_MISMATCHED;

public class TicketQuantityMismatchException extends BusinessException {
    public TicketQuantityMismatchException() {
        super(TICKET_QUANTITY_MISMATCHED);
    }
    public TicketQuantityMismatchException(String message) {
        super(TICKET_QUANTITY_MISMATCHED, message);
    }
}
