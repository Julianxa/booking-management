package com.example.exception.general;

import com.example.exception.BusinessException;

public class RandomReferenceNoException extends BusinessException {
    public RandomReferenceNoException(String message) {
        super("BT008", message);
    }

}
