package com.example.exception;

import com.example.controller.OctoController;
import com.example.exception.octo.OctoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = OctoController.class)
public class OctoExceptionHandler {

    @ExceptionHandler(OctoException.class)
    public ResponseEntity<Map<String, Object>> handleOctoException(OctoException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getError());
        body.put("errorMessage", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }
}
