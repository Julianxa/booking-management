package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResendConfirmationEmailResponseDTO {
    @JsonProperty("success")
    private Boolean success;
    @JsonProperty("message")
    private String message;
    @JsonProperty("booking_event_id")
    private String bookingEventId;
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
