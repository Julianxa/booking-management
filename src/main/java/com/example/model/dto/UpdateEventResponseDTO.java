package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateEventResponseDTO {
    @Schema(description = "Unique identifier of the created event")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Name of the event/activity")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Type of event")
    @JsonProperty("type")
    private String type;

    @Schema(description = "Category or tag")
    @JsonProperty("category")
    private String category;

    @Schema(description = "Detailed description")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Location")
    @JsonProperty("location")
    private String location;

    @Schema(description = "Duration")
    @JsonProperty("duration")
    private Integer duration;

    @Schema(description = "Badge")
    @JsonProperty("badge")
    private String badge;

    @Schema(
            description = "Start date and time of the event (with timezone)"
    )
    @JsonProperty("start_date")
    private LocalDate startDate;

    @Schema(
            description = "End date and time of the event (with timezone)"
    )
    @JsonProperty("end_date")
    private LocalDate endDate;

    @Schema(description = "Explicit list of allowed days. ['MON','TUE','WED','THU','FRI','SAT','SUN']")
    @JsonProperty("available_days")
    private List<AvailableDayDTO> availableDays;

    @Schema(description = "Required equipment")
    @JsonProperty("equipment")
    private String equipment;

    @JsonProperty("event_pic_url")
    private String eventPicUrl;

    @Schema(description = "Availability to employee/staff ratio")
    @JsonProperty("availability_to_employee_ratio")
    private Integer availabilityToEmployeeRatio;

    @Schema(description = "Overall maximum capacity")
    @JsonProperty("max_capacity")
    private Integer maxCapacity;

    @Schema(description = "Whether private/group bookings are allowed")
    @JsonProperty("private_bookings")
    private Boolean privateBookings;

    @Schema(description = "Additional information")
    @JsonProperty("additional_info")
    private String additionalInfo;

    @JsonProperty("custom_question")
    private String customQuestion;

    @JsonProperty("cancellation_policy")
    private String cancellationPolicy;

    @JsonProperty("match_ticket_quantity_with_attendees")
    private Boolean matchTicketQuantityWithAttendees;

    @Schema(description = "Whether the event is published/visible")
    @JsonProperty("is_publish")
    private Boolean isPublish;

    @Schema(description = "Activity duration threshold in days")
    @JsonProperty("activity_day_threshold")
    private Integer activityDayThreshold;

    @Schema(description = "Activity duration threshold in hours")
    @JsonProperty("activity_hour_threshold")
    private Integer activityHourThreshold;

    @Schema(description = "When the event was created")
    @JsonProperty("created_at")
    private ZonedDateTime createdAt;

    @Schema(description = "When the event was updated")
    @JsonProperty("updated_at")
    private ZonedDateTime updatedAt;

    @Schema(description = "Who created the event")
    @JsonProperty("created_by")
    private String createdBy;

    @Schema(description = "Who updated the event")
    @JsonProperty("updated_by")
    private String updatedBy;

    @Schema(description = "Message confirming success", example = "Event created successfully")
    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;
}
