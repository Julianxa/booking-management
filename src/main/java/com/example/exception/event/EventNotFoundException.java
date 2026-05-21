package com.example.exception.event;

import com.example.exception.BusinessException;

public class EventNotFoundException extends BusinessException {
    public EventNotFoundException(String message) {
        super("BT101", message);
    }
}

