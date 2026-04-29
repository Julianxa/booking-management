package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequestDTO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Name of the event/activity")
    @NotBlank(message = "Name is required")
    @JsonProperty("name")
    private String name;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of event")
    @NotBlank(message = "Event Type is required")
    @JsonProperty("type")
    private String type;

    @Schema(description = "Category or tag")
    @JsonProperty("category")
    private String category;

    @Schema(description = "Detailed description of the event")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Physical or virtual location")
    @JsonProperty("location")
    private String location;

    @Schema(description = "Duration in minute(s)")
    @JsonProperty("duration")
    private Integer duration;

    @Schema(description = "Badge")
    @JsonProperty("badge")
    private String badge;

    @Schema(
            description = "Start date of the event",
            example = "2026-07-01"
    )
    @NotNull(message = "Event start date is required")
    @JsonProperty("start_date")
    private LocalDate startDate;

    @Schema(
            description = "End date of the event",
            example = "2026-08-31"
    )
    @JsonProperty("end_date")
    private LocalDate endDate;

    @Schema(description = "Explicit list of allowed days. ['MON','TUE','WED','THU','FRI','SAT','SUN']")
    @JsonProperty("available_days")
    private Set<AvailableDayDTO> availableDays;

    @Schema(description = "Required equipment or materials")
    @JsonProperty("equipment")
    private String equipment;

    @Schema(description = "Ratio of available spots to employees/staff needed")
    @JsonProperty("availability_to_employee_ratio")
    private Integer availabilityToEmployeeRatio;

    @Schema(description = "Overall maximum capacity across all groups")
    @JsonProperty("max_capacity")
    private Integer maxCapacity;

    @Schema(description = "Whether private/group bookings are allowed")
    @JsonProperty("private_bookings")
    private Boolean privateBookings;

    @Schema(description = "Any extra notes or information")
    @JsonProperty("additional_info")
    private String additionalInfo;

    @JsonProperty("match_ticket_quantity_with_attendees")
    private Boolean matchTicketQuantityWithAttendees = true;

    @Schema(description = "Whether the event should be immediately visible")
    @JsonProperty("is_publish")
    private Boolean isPublish;

    @Schema(description = "Minimum cut-off in hours")
    @JsonProperty("min_activity_threshold_time")
    private Double minActivityThresholdTime;

    @Schema(description = "Maximum cut-off in hours")
    @JsonProperty("max_activity_threshold_time")
    private Double maxActivityThresholdTime;
}
