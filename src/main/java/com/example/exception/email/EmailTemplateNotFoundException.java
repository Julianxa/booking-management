package com.example.exception.email;

import com.example.exception.BusinessException;

public class EmailTemplateNotFoundException extends BusinessException {
    public EmailTemplateNotFoundException(String message) {
        super("BT401", message);
    }

}
