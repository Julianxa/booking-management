package com.example.exception.ticket;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.TICKET_PRICE_NOT_FOUND;

public class TicketPricePeriodNotFoundException  extends BusinessException {
    public TicketPricePeriodNotFoundException() {
        super(TICKET_PRICE_NOT_FOUND);
    }
    public TicketPricePeriodNotFoundException(String message) {
        super(TICKET_PRICE_NOT_FOUND, message);
    }
}
