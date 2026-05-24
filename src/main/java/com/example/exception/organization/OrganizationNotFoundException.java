package com.example.exception.organization;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.ORG_NOT_FOUND;

public class OrganizationNotFoundException extends BusinessException {
    public OrganizationNotFoundException() {
        super(ORG_NOT_FOUND);
    }
    public OrganizationNotFoundException(String message) {
        super(ORG_NOT_FOUND, message);
    }
}
