package com.example.exception.giftCertificate;

import com.example.exception.BusinessException;

public class GCPromoCodeExistsException extends BusinessException {
    public GCPromoCodeExistsException(String message) {
        super("BT505", message);
    }

}
