package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @Schema(description = "Name of the event/activity")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Name of the event/activity")
    @JsonProperty("name_zh_cn")
    private String nameZhCn;

    @Schema(description = "Name of the event/activity")
    @JsonProperty("name_zh_hk")
    private String nameZhHk;

    @Schema(description = "Type of event")
    @JsonProperty("type")
    private String type;

    @Schema(description = "Type of event")
    @JsonProperty("type_zh_cn")
    private String typeZhCn;

    @Schema(description = "Type of event")
    @JsonProperty("type_zh_hk")
    private String typeZhHk;

    @Schema(description = "Category or tag")
    @JsonProperty("category")
    private String category;

    @Schema(description = "Category or tag")
    @JsonProperty("category_zh_cn")
    private String categoryZhCn;

    @Schema(description = "Category or tag")
    @JsonProperty("category_zh_hk")
    private String categoryZhHk;

    @Schema(description = "Detailed description of the event")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Detailed description of the event")
    @JsonProperty("description_zh_cn")
    private String descriptionZhCn;

    @Schema(description = "Detailed description of the event")
    @JsonProperty("description_zh_hk")
    private String descriptionZhHk;

    @Schema(description = "Physical or virtual location")
    @JsonProperty("location")
    private String location;

    @Schema(description = "Physical or virtual location")
    @JsonProperty("location_zh_cn")
    private String locationZhCn;

    @Schema(description = "Physical or virtual location")
    @JsonProperty("location_zh_hk")
    private String locationZhHk;

    @Schema(description = "Duration in minute(s)")
    @JsonProperty("duration")
    private Integer duration;

    @Schema(description = "Badge")
    @JsonProperty("badge")
    private String badge;

    @Schema(description = "Badge")
    @JsonProperty("badge_zh_cn")
    private String badgeZhCn;

    @Schema(description = "Badge")
    @JsonProperty("badge_zh_hk")
    private String badgeZhHk;

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

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Explicit list of allowed days. ['MON','TUE','WED','THU','FRI','SAT','SUN']")
    @JsonProperty("available_days")
    private Set<AvailableDayDTO> availableDays;

    @Schema(description = "Required equipment or materials")
    @JsonProperty("equipment")
    private String equipment;

    @Schema(description = "Required equipment or materials")
    @JsonProperty("equipment_zh_cn")
    private String equipmentZhCn;

    @Schema(description = "Required equipment or materials")
    @JsonProperty("equipment_zh_hk")
    private String equipmentZhHk;

    @Schema(description = "Ratio of available spots to employees/staff needed")
    @JsonProperty("availability_to_employee_ratio")
    private Integer availabilityToEmployeeRatio;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Overall maximum capacity across all groups")
    @JsonProperty("max_capacity")
    private Integer maxCapacity;

    @Schema(description = "Whether private/group bookings are allowed")
    @JsonProperty("private_bookings")
    private Boolean privateBookings;

    @Schema(description = "Any extra notes or information")
    @JsonProperty("additional_info")
    private String additionalInfo;

    @Schema(description = "Any extra notes or information")
    @JsonProperty("additional_info_zh_cn")
    private String additionalInfoZhCn;

    @Schema(description = "Any extra notes or information")
    @JsonProperty("additional_info_zh_hk")
    private String additionalInfoZhHk;

    @JsonProperty("cancellation_policy")
    private String cancellationPolicy;

    @JsonProperty("cancellation_policy_zh_cn")
    private String cancellationPolicyZhCn;

    @JsonProperty("cancellation_policy_zh_hk")
    private String cancellationPolicyZhHk;

    @JsonProperty("custom_question")
    private String customQuestion;

    @JsonProperty("custom_question_zh_cn")
    private String customQuestionZhCn;

    @JsonProperty("custom_question_zh_hk")
    private String customQuestionZhHk;

    @Schema(description = "Display/order sequence number (positive integer)")
    @Positive(message = "sequence_no must be a positive integer")
    @JsonProperty("sequence_no")
    private Integer sequenceNo;

    @JsonProperty("match_ticket_quantity_with_attendees")
    private Boolean matchTicketQuantityWithAttendees = true;

    @NotNull(message = "is_publish is required")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Whether the event should be immediately visible")
    @JsonProperty("is_publish")
    private Boolean isPublish;

    @Schema(description = "Minimum lead time in days before the event date when new bookings are blocked")
    @JsonProperty("min_activity_day_threshold")
    private Integer minActivityDayThreshold;

    @Schema(description = "Minimum lead time in hours before the event start when new bookings are blocked")
    @JsonProperty("min_activity_hour_threshold")
    private Integer minActivityHourThreshold;

    @Schema(description = "Maximum lead time in days before the event date when new bookings become available")
    @JsonProperty("max_activity_day_threshold")
    private Integer maxActivityDayThreshold;

    @Schema(description = "Maximum lead time in hours before the event start when new bookings become available")
    @JsonProperty("max_activity_hour_threshold")
    private Integer maxActivityHourThreshold;

    @JsonProperty("email_template_id")
    private String emailTemplateId;
}
