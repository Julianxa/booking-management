package com.example.model.dto;


import com.example.jackson.AbstractPartialUpdateDto;
import com.example.jackson.PartialUpdate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@PartialUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateEventRequestDTO extends AbstractPartialUpdateDto {
    @Schema(description = "Display/order sequence number (positive integer)")
    @JsonProperty("sequence_no")
    private Integer sequenceNo;

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

    @Schema(description = "Start date and time of the event (with timezone)", example = "2026-07-01")
    @JsonProperty("start_date")
    private LocalDate startDate;

    @Schema(description = "End date and time of the event (with timezone)", example = "2026-08-31")
    @JsonProperty("end_date")
    private LocalDate endDate;

    @Schema(description = "Explicit list of allowed days. ['MON','TUE','WED','THU','FRI','SAT','SUN']")
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

    @Schema(description = "Overall maximum capacity across all groups")
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

    @JsonProperty("match_ticket_quantity_with_attendees")
    private Boolean matchTicketQuantityWithAttendees;

    @Schema(description = "Whether the event should be immediately visible")
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

    @Schema(description = "Email template reference number")
    @JsonProperty("email_template_id")
    private String emailTemplateId;

    @Schema(
            description =
                    "Existing event picture keys to keep (from GET response). Omit = no change; [] = remove all; send full list to drop specific images.")
    @JsonProperty("event_pic_keys")
    private List<String> eventPicKeys;

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
