package com.example.exception.email;

import com.example.exception.BusinessException;

public class EmailProcessException extends BusinessException {
    public EmailProcessException(String message) {
        super("BT401", message);
    }

}
