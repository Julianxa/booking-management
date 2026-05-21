package com.example.exception.general;

import com.example.exception.BusinessException;

public class InvalidJsonFormatException extends BusinessException {
    public InvalidJsonFormatException(String message) {
        super("BT004", message);
    }

}
