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

    @Schema(description = "Detailed description")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Detailed description of the event")
    @JsonProperty("description_zh_cn")
    private String descriptionZhCn;

    @Schema(description = "Detailed description of the event")
    @JsonProperty("description_zh_hk")
    private String descriptionZhHk;

    @Schema(description = "Location")
    @JsonProperty("location")
    private String location;

    @Schema(description = "Physical or virtual location")
    @JsonProperty("location_zh_cn")
    private String locationZhCn;

    @Schema(description = "Physical or virtual location")
    @JsonProperty("location_zh_hk")
    private String locationZhHk;

    @Schema(description = "Duration")
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

    @Schema(description = "Required equipment or materials")
    @JsonProperty("equipment_zh_cn")
    private String equipmentZhCn;

    @Schema(description = "Required equipment or materials")
    @JsonProperty("equipment_zh_hk")
    private String equipmentZhHk;

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

    @Schema(description = "Any extra notes or information")
    @JsonProperty("additional_info_zh_cn")
    private String additionalInfoZhCn;

    @Schema(description = "Any extra notes or information")
    @JsonProperty("additional_info_zh_hk")
    private String additionalInfoZhHk;

    @JsonProperty("custom_question")
    private String customQuestion;

    @JsonProperty("custom_question_zh_cn")
    private String customQuestionZhCn;

    @JsonProperty("custom_question_zh_hk")
    private String customQuestionZhHk;

    @JsonProperty("cancellation_policy")
    private String cancellationPolicy;

    @JsonProperty("cancellation_policy_zh_cn")
    private String cancellationPolicyZhCn;

    @JsonProperty("cancellation_policy_zh_hk")
    private String cancellationPolicyZhHk;

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

    @Schema(description = "Associated email template")
    @JsonProperty("email_template")
    private GetEmailTemplateResponseDTO emailTemplate;

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
