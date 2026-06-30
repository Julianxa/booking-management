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
public class ReportSummaryResponseDTO {
    @JsonProperty("id")
    private String id;

    @JsonProperty("report_type")
    private Enums.ReportType reportType;

    @JsonProperty("s3_key")
    private String s3Key;

    @JsonProperty("download_url")
    private String downloadUrl;

    @JsonProperty("report_start_date")
    private LocalDate reportStartDate;

    @JsonProperty("report_end_date")
    private LocalDate reportEndDate;

    @JsonProperty("generated_by")
    private String generatedBy;

    @JsonProperty("included_booking_events")
    private Integer includedBookingEvents;

    @JsonProperty("total_booking_events_in_range")
    private Long totalBookingEventsInRange;

    @JsonProperty("file_size_bytes")
    private Long fileSizeBytes;

    @JsonProperty("created_at")
    private ZonedDateTime createdAt;
}
