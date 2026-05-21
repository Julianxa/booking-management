package com.example.exception.general;

import com.example.exception.BusinessException;

public class FileUploadException extends BusinessException {
    public FileUploadException(String message) {
        super("BT002", message);
    }

}
