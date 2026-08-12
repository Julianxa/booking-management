package com.example.exception.octo;

import org.springframework.http.HttpStatus;

public class OctoException extends RuntimeException {
    private final String error;
    private final HttpStatus httpStatus;

    public OctoException(String error, String message, HttpStatus httpStatus) {
        super(message);
        this.error = error;
        this.httpStatus = httpStatus;
    }

    public String getError() {
        return error;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public static OctoException notFound(String error, String message) {
        return new OctoException(error, message, HttpStatus.BAD_REQUEST);
    }

    public static OctoException badRequest(String error, String message) {
        return new OctoException(error, message, HttpStatus.BAD_REQUEST);
    }

    public static OctoException conflict(String error, String message) {
        return new OctoException(error, message, HttpStatus.CONFLICT);
    }
}
