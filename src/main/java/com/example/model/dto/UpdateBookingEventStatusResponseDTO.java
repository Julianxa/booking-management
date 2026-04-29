package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateBookingEventStatusResponseDTO {
    @JsonProperty("id")
    private String id;

    @JsonProperty("booking_id")
    private String bookingId;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_date")
    private LocalDate eventDate;

    @JsonProperty("event_time")
    private String eventTime;

    @JsonProperty("status")
    private Enums.BookingEventStatus status;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
}
