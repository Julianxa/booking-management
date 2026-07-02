package com.example.model.dto;

import com.example.constant.Enums;
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
    @JsonProperty("id")
    private String id;

    @JsonProperty("s3_key")
    private String s3Key;

    @JsonProperty("download_url")
    private String downloadUrl;

    @JsonProperty("report_start_date")
    private LocalDate reportStartDate;

    @JsonProperty("report_end_date")
    private LocalDate reportEndDate;

    @JsonProperty("included_booking_events")
    private int includedBookingEvents;

    @JsonProperty("total_booking_events_in_range")
    private Long totalBookingEventsInRange;

    @JsonProperty("status")
    private Enums.ReportStatus status;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;
}
