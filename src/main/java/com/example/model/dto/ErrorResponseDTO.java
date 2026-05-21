package com.example.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ErrorResponseDTO {
    private String errorCode;
    private String message;
    private String timestamp;
    private String traceId;
}