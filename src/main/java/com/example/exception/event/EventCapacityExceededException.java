package com.example.exception.event;

import com.example.exception.BusinessException;

public class EventCapacityExceededException extends BusinessException {

    public EventCapacityExceededException(String message) {
        super("BT601", message);
    }
}