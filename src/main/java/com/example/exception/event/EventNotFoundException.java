package com.example.exception.event;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.EVENT_NOT_FOUND;

public class EventNotFoundException extends BusinessException {
    public EventNotFoundException() {
        super(EVENT_NOT_FOUND);
    }
    public EventNotFoundException(String message) {
        super(EVENT_NOT_FOUND, message);
    }
}

