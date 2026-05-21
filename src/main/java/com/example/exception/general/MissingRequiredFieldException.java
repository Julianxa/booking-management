package com.example.exception.general;

import com.example.exception.BusinessException;

public class MissingRequiredFieldException extends BusinessException {
    public MissingRequiredFieldException(String message) {
        super("BT007", message);
    }
}