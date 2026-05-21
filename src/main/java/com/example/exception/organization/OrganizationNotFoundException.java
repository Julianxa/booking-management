package com.example.exception.organization;

import com.example.exception.BusinessException;

public class OrganizationNotFoundException extends BusinessException {
    public OrganizationNotFoundException(String message) {
        super("BT501", message);
    }

}
