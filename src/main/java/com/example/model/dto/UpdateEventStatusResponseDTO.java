package com.example.model.dto;

import com.example.constant.Enums;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateEventStatusResponseDTO {
    @JsonProperty("event_ref_no")
    private String eventRefNo;
    @JsonProperty("event_date")
    private LocalDate eventDate;
    @JsonProperty("event_time")
    private String eventTime;
    @JsonProperty("status")
    private Enums.EventStatus status;
    @JsonProperty("closed_at")
    private ZonedDateTime closedAt;
    @JsonProperty("message")
    private String message;
    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;
}
