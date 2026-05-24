package com.example.exception.general;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.QR_CODE_GENERATION_ERROR;

public class GenerateQRCodeException extends BusinessException {
    public GenerateQRCodeException() {
        super(QR_CODE_GENERATION_ERROR);
    }

    public GenerateQRCodeException(String message) {
        super(QR_CODE_GENERATION_ERROR, message);
    }
}
