package com.example.exception.email;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.EMAIL_TEMPLATE_NAME_EXISTS;

public class EmailTemplateNameExistsException extends BusinessException {
    public EmailTemplateNameExistsException() {
        super(EMAIL_TEMPLATE_NAME_EXISTS);
    }

    public EmailTemplateNameExistsException(String message) {
        super(EMAIL_TEMPLATE_NAME_EXISTS, message);
    }
}
