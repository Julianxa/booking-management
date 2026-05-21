package com.example.exception.general;

import com.example.exception.BusinessException;

public class GenerateHashException extends BusinessException {
    public GenerateHashException(String message) {
        super("BT003", message);
    }

}
