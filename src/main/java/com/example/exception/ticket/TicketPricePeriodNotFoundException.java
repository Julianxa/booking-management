package com.example.exception.ticket;

import com.example.exception.BusinessException;

public class TicketPricePeriodNotFoundException  extends BusinessException {
    public TicketPricePeriodNotFoundException(String message) {
        super("BT202", message);
    }
}
