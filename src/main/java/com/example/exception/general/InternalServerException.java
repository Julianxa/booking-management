package com.example.exception.general;

import com.example.exception.BusinessException;

public class InternalServerException extends BusinessException {
    public InternalServerException(String message) {
        super("BT002", message);
    }
}
