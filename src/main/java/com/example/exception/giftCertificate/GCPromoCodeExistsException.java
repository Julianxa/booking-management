package com.example.exception.giftCertificate;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.GC_PROMO_CODE_EXISTS;

public class GCPromoCodeExistsException extends BusinessException {
    public GCPromoCodeExistsException() {
        super(GC_PROMO_CODE_EXISTS);
    }
    public GCPromoCodeExistsException(String message) {
        super(GC_PROMO_CODE_EXISTS, message);
    }
}
