package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmCheckinRequestDTO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Booking ID is required")
    @JsonProperty("booking_id")
    private String bookingId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Event ID is required")
    @JsonProperty("event_id")
    private String eventId;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Date of the event",
            example = "2026-07-01"
    )
    @NotNull
    @JsonProperty("event_date")
    private LocalDate eventDate;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Time of the event",
            example = "17:00"
    )
    @NotBlank(message = "Event time is required")
    @JsonProperty("event_time")
    private String eventTime;
}
