package com.example.exception.booking;

import com.example.exception.BusinessException;

public class SeatOrTicketTakenException extends BusinessException {
    public SeatOrTicketTakenException(String message) {
        super("BT306", message);
    }
}
