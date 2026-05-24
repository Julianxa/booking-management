package com.example.exception.giftCertificate;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.GC_REDEMPTION_NOT_FOUND;

public class GCRedemptionNotFoundException extends BusinessException {
    public GCRedemptionNotFoundException() {
        super(GC_REDEMPTION_NOT_FOUND);
    }

    public GCRedemptionNotFoundException(String message) {
        super(GC_REDEMPTION_NOT_FOUND, message);
    }
}
