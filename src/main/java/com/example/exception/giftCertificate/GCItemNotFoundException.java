package com.example.exception.giftCertificate;

import com.example.exception.BusinessException;

public class GCItemNotFoundException extends BusinessException {
    public GCItemNotFoundException(String message) {
        super("BT504", message);
    }

}
