package com.example.exception.email;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.EMAIL_TEMPLATE_NOT_FOUND;

public class EmailTemplateNotFoundException extends BusinessException {
    public EmailTemplateNotFoundException() {
        super(EMAIL_TEMPLATE_NOT_FOUND);
    }
    public EmailTemplateNotFoundException(String message) {
        super(EMAIL_TEMPLATE_NOT_FOUND, message);
    }

}
