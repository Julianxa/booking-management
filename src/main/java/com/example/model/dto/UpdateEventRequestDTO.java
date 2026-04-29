package com.example.model.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class UpdateEventRequestDTO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Name of the event/activity")
    @NotBlank(message = "Name is required")
    @JsonProperty("name")
    private String name;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Type of event")
    @NotBlank(message = "Type is required")
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
            description = "Start date and time of the event (with timezone)",
            example = "2026-07-01"
    )
    @JsonProperty("start_date")
    private LocalDate startDate;

    @Schema(
            description = "End date and time of the event (with timezone)",
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

    @Schema(description = "Whether the event should be immediately visible")
    @JsonProperty("is_publish")
    private Boolean isPublish;

    @Schema(description = "Minimum activity duration threshold in hours")
    @JsonProperty("min_activity_threshold_time")
    private Double minActivityThresholdTime;

    @Schema(description = "Maximum activity duration threshold in hours")
    @JsonProperty("max_activity_threshold_time")
    private Double maxActivityThresholdTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "ParticipantGroup", description = "One participant group/category")
    public static class ParticipantGroupDTO {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Group name (e.g. 'Child', 'Adult')",
                example = "Child")
        @NotBlank(message = "Group name is required")
        @JsonProperty("name")
        private String name;

        @Schema(description = "Short description of this group")
        @JsonProperty("description")
        private String description;

        @Schema(description = "Minimum age for this group")
        @Min(0)
        @JsonProperty("min_age")
        private Integer minAge;

        @Schema(description = "Maximum age for this group (null = no upper limit)")
        @JsonProperty("max_age")
        private Integer maxAge;

        @Schema(description = "Maximum number of participants allowed in this group")
        @Min(1)
        @JsonProperty("max_spots")
        private Integer maxSpots;

        @Schema(description = "Price per participant in this group (HKD)", example = "1200.00")
        @JsonProperty("price")
        private Double price;

        @Schema(description = "Currency code", example = "HKD", defaultValue = "HKD")
        @JsonProperty("currency")
        private String currency = "HKD";
    }
}
