package com.example.exception.general;

import com.example.exception.BusinessException;

import static com.example.exception.ErrorCode.FILE_UPLOAD_ERROR;

public class FileUploadException extends BusinessException {
    public FileUploadException() {
        super(FILE_UPLOAD_ERROR);
    }
    public FileUploadException(String message) {
        super(FILE_UPLOAD_ERROR, message);
    }
}
