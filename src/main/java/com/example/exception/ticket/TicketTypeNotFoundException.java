package com.example.exception.ticket;

import com.example.exception.BusinessException;

public class TicketTypeNotFoundException extends BusinessException {
    public TicketTypeNotFoundException(String message) {
        super("BT903", message);
    }
}
