package com.example.exception.ticket;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.TICKET_TYPE_NOT_FOUND;

public class TicketTypeNotFoundException extends BusinessException {
    public TicketTypeNotFoundException() {
        super(TICKET_TYPE_NOT_FOUND);
    }
    public TicketTypeNotFoundException(String message) {
        super(TICKET_TYPE_NOT_FOUND, message);
    }
}
