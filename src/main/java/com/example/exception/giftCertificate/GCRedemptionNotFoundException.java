package com.example.exception.giftCertificate;

import com.example.exception.BusinessException;

public class GCRedemptionNotFoundException extends BusinessException {
    public GCRedemptionNotFoundException(String message) {
        super("BT501", message);
    }
}
