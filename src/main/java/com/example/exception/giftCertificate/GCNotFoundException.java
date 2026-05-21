package com.example.exception.giftCertificate;

import com.example.exception.BusinessException;

public class GCNotFoundException extends BusinessException {
    public GCNotFoundException(String message) {
        super("BT501", message);
    }

}
