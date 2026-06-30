package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerateBookingsByActivityDateReportResponseDTO {
    @JsonProperty("s3_key")
    private String s3Key;

    @JsonProperty("download_url")
    private String downloadUrl;

    @JsonProperty("report_start_date")
    private LocalDate reportStartDate;

    @JsonProperty("report_end_date")
    private LocalDate reportEndDate;

    @JsonProperty("row_count")
    private int rowCount;

    @JsonProperty("booking_events_in_range")
    private Long bookingEventsInRange;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;
}
